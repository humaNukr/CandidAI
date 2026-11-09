package ua.edu.ukma.candidai.common.audit.model;

import java.util.UUID;
import lombok.Builder;

@Builder
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
        Long executionTimeMs,
        boolean success,
        String errorMessage,
        String exceptionClass
) {

    public AuditLogEntity toEntity() {
        return AuditLogEntity.builder()
                .action(action)
                .className(className)
                .methodName(methodName)
                .userId(userId)
                .userEmail(userEmail)
                .userRole(userRole)
                .traceId(traceId)
                .clientIp(clientIp)
                .parameters(parameters)
                .executionTimeMs(executionTimeMs != null ? executionTimeMs : 0L)
                .success(success)
                .errorMessage(errorMessage)
                .exceptionClass(exceptionClass)
                .build();
    }
}
