package ua.edu.ukma.candidai.common.audit.model;

import java.time.Instant;
import java.util.UUID;

public record AuditLogRecord(
        String action,
        String className,
        String methodName,
        UUID userId,
        String userEmail,
        String userRole,
        String traceId,
        String clientIp,
        String parameters,
        long executionTimeMs,
        boolean success,
        String errorMessage,
        String exceptionClass,
        Instant createdAt
) {
}
