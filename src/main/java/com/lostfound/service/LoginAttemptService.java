
package com.lostfound.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MILLIS = 15 * 60 * 1000L;
    private static final long LOCK_MILLIS = 15 * 60 * 1000L;

    private final ConcurrentMap<String, AttemptState> attempts =
            new ConcurrentHashMap<>();

    public void checkBlocked(String identifier) {
        String key = normalize(identifier);
        AttemptState state = attempts.get(key);

        if (state == null) {
            return;
        }

        synchronized (state) {
            long now = System.currentTimeMillis();

            if (state.lockedUntil > now) {
                long minutesLeft =
                        Math.max(1,
                                (state.lockedUntil - now + 59999) / 60000);

                throw new IllegalArgumentException(
                        "Too many failed login attempts. Try again in "
                                + minutesLeft + " minute(s).");
            }

            if (state.lockedUntil != 0 && state.lockedUntil <= now) {
                attempts.remove(key, state);
                return;
            }

            if (now - state.windowStartedAt >= WINDOW_MILLIS) {
                attempts.remove(key, state);
            }
        }
    }

    public void recordFailure(String identifier) {
        String key = normalize(identifier);
        long now = System.currentTimeMillis();

        AttemptState state = attempts.computeIfAbsent(
                key, ignored -> new AttemptState(now));

        synchronized (state) {
            if (now - state.windowStartedAt >= WINDOW_MILLIS) {
                state.windowStartedAt = now;
                state.failures = 0;
            }

            state.failures++;

            if (state.failures >= MAX_ATTEMPTS) {
                state.lockedUntil = now + LOCK_MILLIS;
            }
        }
    }

    public void reset(String identifier) {
        attempts.remove(normalize(identifier));
    }

    private String normalize(String identifier) {
        return identifier == null
                ? ""
                : identifier.trim().toLowerCase();
    }

    private static class AttemptState {
        private long windowStartedAt;
        private int failures;
        private long lockedUntil;

        private AttemptState(long now) {
            this.windowStartedAt = now;
        }
    }
}
