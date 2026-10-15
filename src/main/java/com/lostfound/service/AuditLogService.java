package com.lostfound.service;

import com.lostfound.entity.AuditLog;
import com.lostfound.repository.AuditLogRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(
            AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEvent(
            String eventType,
            String actorIdentifier,
            String details,
            String ipAddress) {

        AuditLog log = new AuditLog();

        log.setEventType(safeValue(eventType, 60));

        log.setActorIdentifier(
                safeValue(maskIdentifier(eventType, actorIdentifier), 255));

        log.setDetails(safeValue(details, 1000));
        log.setIpAddress(safeValue(ipAddress, 100));

        auditLogRepository.save(log);
    }

    private String maskIdentifier(
            String eventType,
            String identifier) {

        if (identifier == null || identifier.isBlank()) {
            return null;
        }

        String value = identifier.trim();

        // Mask email addresses.
        int atIndex = value.indexOf('@');

        if (atIndex > 0 && atIndex < value.length() - 1) {
            String localPart = value.substring(0, atIndex);
            String domain = value.substring(atIndex + 1);

            String maskedLocal;

            if (localPart.length() == 1) {
                maskedLocal = "*";
            } else {
                maskedLocal = localPart.charAt(0) + "***";
            }

            return maskedLocal + "@" + domain;
        }

        // Rate-limit events use the IP as the actor identifier.
        // Preserve it here rather than mistaking it for a phone number.
        if ("RATE_LIMIT_EXCEEDED".equalsIgnoreCase(eventType)) {
            return value;
        }

        // Mask phone numbers while preserving the country code
        // and the final four digits where possible.
        String digits = value.replaceAll("\\D", "");

        if (digits.length() >= 7 && digits.length() <= 15
                && value.matches("[+0-9()\\s-]+")) {

            int visibleDigits = Math.min(4, digits.length());
            int hiddenDigits = digits.length() - visibleDigits;

            StringBuilder masked = new StringBuilder();

            for (int i = 0; i < hiddenDigits; i++) {
                masked.append('*');
            }

            masked.append(digits.substring(hiddenDigits));

            return masked.toString();
        }

        return value;
    }

    private String safeValue(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String cleaned = value.trim()
                .replace('\n', ' ')
                .replace('\r', ' ');

        return cleaned.length() > maxLength
                ? cleaned.substring(0, maxLength)
                : cleaned;
    }
}
