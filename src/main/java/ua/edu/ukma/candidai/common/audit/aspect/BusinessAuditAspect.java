package ua.edu.ukma.candidai.common.audit.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import ua.edu.ukma.candidai.common.audit.annotation.AuditAction;
import ua.edu.ukma.candidai.common.audit.annotation.AuditSensitive;
import ua.edu.ukma.candidai.common.audit.model.AuditLogRecord;
import ua.edu.ukma.candidai.common.audit.service.AuditLogPersistenceService;
import ua.edu.ukma.candidai.common.logging.TraceIdFilter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Aspect
@Component
@Order(20)
public class BusinessAuditAspect {

    private static final Logger LOG = LoggerFactory.getLogger(BusinessAuditAspect.class);
    private static final int TRACE_ID_PREFIX_LENGTH = 8;

    private final ObjectMapper objectMapper;
    private final AuditLogPersistenceService auditLogPersistenceService;

    public BusinessAuditAspect(
            ObjectMapper objectMapper,
            @Autowired(required = false) @Nullable AuditLogPersistenceService auditLogPersistenceService
    ) {
        this.objectMapper = objectMapper;
        this.auditLogPersistenceService = auditLogPersistenceService;
    }

    @Around("@annotation(auditAction)")
    public Object auditOperation(ProceedingJoinPoint joinPoint, AuditAction auditAction) throws Throwable {
        long startTime = System.currentTimeMillis();
        AuditActor actor = resolveActor();
        String traceId = resolveTraceId();
        String clientIp = resolveClientIp();
        String parametersJson = extractAndMaskParameters(joinPoint, auditAction);

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        boolean isSuccess = false;
        String errorMessage = null;
        String exceptionClass = null;

        try {
            Object result = joinPoint.proceed();
            isSuccess = true;
            return result;
        } catch (Throwable ex) {
            isSuccess = false;
            errorMessage = ex.getMessage();
            exceptionClass = ex.getClass().getName();
            throw ex;
        } finally {
            long executionTimeMs = System.currentTimeMillis() - startTime;
            AuditLogRecord record = new AuditLogRecord(
                    auditAction.action(),
                    className,
                    methodName,
                    actor.userId(),
                    actor.userEmail(),
                    actor.userRole(),
                    traceId,
                    clientIp,
                    parametersJson,
                    executionTimeMs,
                    isSuccess,
                    errorMessage,
                    exceptionClass,
                    Instant.now()
            );
            persistSafely(record);
        }
    }

    private void persistSafely(AuditLogRecord record) {
        if (auditLogPersistenceService == null) {
            return;
        }
        try {
            auditLogPersistenceService.saveAuditLog(record);
        } catch (Exception ex) {
            LOG.error("Failed to persist audit log record for action {}", record.action(), ex);
        }
    }

    private String extractAndMaskParameters(ProceedingJoinPoint joinPoint, AuditAction auditAction) {
        if (!auditAction.logParameters()) {
            return null;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (args == null || args.length == 0) {
            return "{}";
        }

        Map<String, Object> paramsMap = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String paramName = (parameterNames != null && parameterNames.length > i)
                    ? parameterNames[i] : parameters[i].getName();
            Object arg = args[i];

            if (parameters[i].isAnnotationPresent(AuditSensitive.class)) {
                AuditSensitive sensitive = parameters[i].getAnnotation(AuditSensitive.class);
                paramsMap.put(paramName, sensitive.mask());
            } else if (arg == null) {
                paramsMap.put(paramName, null);
            } else {
                paramsMap.put(paramName, maskObjectFields(arg));
            }
        }

        try {
            return objectMapper.writeValueAsString(paramsMap);
        } catch (Exception ex) {
            return "{\"error\":\"Serialization failed: " + ex.getMessage() + "\"}";
        }
    }

    private Object maskObjectFields(Object arg) {
        try {
            JsonNode node = objectMapper.valueToTree(arg);
            if (node.isObject()) {
                ObjectNode objectNode = (ObjectNode) node;
                maskFieldsRecursively(arg.getClass(), objectNode);
                return objectNode;
            }
            return node;
        } catch (Exception ex) {
            return String.valueOf(arg);
        }
    }

    private void maskFieldsRecursively(Class<?> clazz, ObjectNode objectNode) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                AuditSensitive sensitive = field.getAnnotation(AuditSensitive.class);
                if (sensitive != null && objectNode.has(field.getName())) {
                    objectNode.put(field.getName(), sensitive.mask());
                }
            }
            current = current.getSuperclass();
        }
    }

    private AuditActor resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return new AuditActor(null, "ANONYMOUS", "ROLE_ANONYMOUS");
        }

        UUID userId = null;
        String userEmail = authentication.getName();
        String userRole = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_ANONYMOUS");

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            userEmail = userDetails.getUsername();
        } else {
            try {
                Method getEmailMethod = principal.getClass().getMethod("getEmail");
                Object emailVal = getEmailMethod.invoke(principal);
                if (emailVal instanceof String emailStr) {
                    userEmail = emailStr;
                }
            } catch (ReflectiveOperationException ignored) {
                userEmail = authentication.getName();
            }
        }

        try {
            Method getIdMethod = principal.getClass().getMethod("getId");
            Object idVal = getIdMethod.invoke(principal);
            if (idVal instanceof UUID uuid) {
                userId = uuid;
            }
        } catch (ReflectiveOperationException ignored) {
            userId = null;
        }

        return new AuditActor(userId, userEmail, userRole);
    }

    private String resolveTraceId() {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        return traceId != null ? traceId : UUID.randomUUID().toString().substring(0, TRACE_ID_PREFIX_LENGTH);
    }

    private String resolveClientIp() {
        String clientIp = MDC.get(TraceIdFilter.CLIENT_IP_MDC_KEY);
        return clientIp != null ? clientIp : "UNKNOWN";
    }

    private record AuditActor(UUID userId, String userEmail, String userRole) {}
}

