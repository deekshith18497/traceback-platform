
package com.lostfound.security;

import com.lostfound.service.AuditLogService;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int CAPACITY = 100;
    private static final Duration REFILL_PERIOD =
            Duration.ofMinutes(1);

    private static final long AUDIT_INTERVAL_MILLIS =
            Duration.ofMinutes(1).toMillis();

    private final ConcurrentMap<String, Bucket> buckets =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Long> lastAuditTimes =
            new ConcurrentHashMap<>();

    private final AuditLogService auditLogService;

    public RateLimitingFilter(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.classic(
                CAPACITY,
                Refill.intervally(100, REFILL_PERIOD)
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket resolveBucket(String key) {
        return buckets.computeIfAbsent(
                key,
                ignored -> createBucket()
        );
    }

    private void recordRateLimitEvent(
            String clientIp,
            HttpServletRequest request) {

        long now = System.currentTimeMillis();

        // Atomically allow only one audit attempt per IP
        // during each one-minute interval.
        boolean shouldAudit = lastAuditTimes.compute(
                clientIp,
                (ip, previousTime) ->
                        previousTime == null
                                || now - previousTime
                                        >= AUDIT_INTERVAL_MILLIS
                                ? now
                                : previousTime
        ) == now;

        if (!shouldAudit) {
            return;
        }

        try {
            auditLogService.recordEvent(
                    "RATE_LIMIT_EXCEEDED",
                    clientIp,
                    "Request limit exceeded for "
                            + request.getMethod()
                            + " " + request.getRequestURI(),
                    clientIp
            );
        } catch (Exception exception) {
            System.err.println(
                    "Unable to save rate-limit audit event");
        }
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String clientIp = request.getRemoteAddr();
        Bucket bucket = resolveBucket(clientIp);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        recordRateLimitEvent(clientIp, request);

        response.setStatus(429);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                "{\"status\":429,\"error\":\"Too Many Requests\","
                + "\"message\":\"Request limit exceeded. Please try again later.\"}"
        );
    }
}
