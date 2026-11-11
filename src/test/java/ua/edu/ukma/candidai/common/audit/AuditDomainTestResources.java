package ua.edu.ukma.candidai.common.audit;

import java.time.Instant;
import java.util.UUID;
import ua.edu.ukma.candidai.common.audit.model.AuditLogEntity;
import ua.edu.ukma.candidai.common.audit.model.AuditLogRecord;

public final class AuditDomainTestResources {

    public static final UUID AUDIT_ID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID USER_ID_RECRUITER = UUID.fromString("e0000000-0000-0000-0000-000000000001");
    public static final UUID USER_ID_CANDIDATE = UUID.fromString("c0000000-0000-0000-0000-000000000001");

    public static final String ACTION_CREATE_VACANCY = "CREATE_VACANCY";
    public static final String ACTION_APPLY_VACANCY = "APPLY_VACANCY";
    public static final String CLASS_VACANCY_SERVICE = "VacancyServiceImpl";
    public static final String CLASS_APPLICATION_SERVICE = "ApplicationServiceImpl";
    public static final String METHOD_CREATE_VACANCY = "createVacancy";
    public static final String METHOD_APPLY = "apply";

    public static final String EMAIL_RECRUITER = "recruiter@candidai.com";
    public static final String EMAIL_CANDIDATE = "candidate@candidai.com";
    public static final String ROLE_RECRUITER = "ROLE_RECRUITER";
    public static final String ROLE_CANDIDATE = "ROLE_CANDIDATE";

    public static final String TRACE_ID_SAMPLE = "4f8b2c1a";
    public static final String CLIENT_IP_SAMPLE = "192.168.1.10";
    public static final String PARAMS_JSON_SAMPLE = "{\"title\":\"Senior Java Engineer\"}";
    public static final long EXECUTION_TIME_MS = 42L;
    public static final String ERROR_DUPLICATE_VACANCY = "Active vacancy with title already exists";
    public static final String EXCEPTION_DUPLICATE_CLASS
            = "ua.edu.ukma.candidai.common.exception.DuplicateResourceException";
    public static final Instant FIXED_CREATED_AT = Instant.parse("2026-11-09T12:00:00Z");

    private AuditDomainTestResources() {
    }

    public static AuditLogRecord successfulAuditRecord() {
        return AuditLogRecord.builder()
                .action(ACTION_CREATE_VACANCY)
                .className(CLASS_VACANCY_SERVICE)
                .methodName(METHOD_CREATE_VACANCY)
                .userId(USER_ID_RECRUITER)
                .userEmail(EMAIL_RECRUITER)
                .userRole(ROLE_RECRUITER)
                .traceId(TRACE_ID_SAMPLE)
                .clientIp(CLIENT_IP_SAMPLE)
                .parameters(PARAMS_JSON_SAMPLE)
                .executionTimeMs(EXECUTION_TIME_MS)
                .success(true)
                .errorMessage(null)
                .exceptionClass(null)
                .build();
    }

    public static AuditLogRecord failedAuditRecord() {
        return AuditLogRecord.builder()
                .action(ACTION_CREATE_VACANCY)
                .className(CLASS_VACANCY_SERVICE)
                .methodName(METHOD_CREATE_VACANCY)
                .userId(USER_ID_RECRUITER)
                .userEmail(EMAIL_RECRUITER)
                .userRole(ROLE_RECRUITER)
                .traceId(TRACE_ID_SAMPLE)
                .clientIp(CLIENT_IP_SAMPLE)
                .parameters(PARAMS_JSON_SAMPLE)
                .executionTimeMs(EXECUTION_TIME_MS)
                .success(false)
                .errorMessage(ERROR_DUPLICATE_VACANCY)
                .exceptionClass(EXCEPTION_DUPLICATE_CLASS)
                .build();
    }

    public static AuditLogRecord candidateApplyAuditRecord() {
        return AuditLogRecord.builder()
                .action(ACTION_APPLY_VACANCY)
                .className(CLASS_APPLICATION_SERVICE)
                .methodName(METHOD_APPLY)
                .userId(USER_ID_CANDIDATE)
                .userEmail(EMAIL_CANDIDATE)
                .userRole(ROLE_CANDIDATE)
                .traceId(TRACE_ID_SAMPLE)
                .clientIp(CLIENT_IP_SAMPLE)
                .parameters("{\"vacancyId\":\"b0000000-0000-0000-0000-000000000001\"}")
                .executionTimeMs(15L)
                .success(true)
                .errorMessage(null)
                .exceptionClass(null)
                .build();
    }

    public static AuditLogEntity expectedSuccessfulAuditEntity() {
        return AuditLogEntity.builder()
                .action(ACTION_CREATE_VACANCY)
                .className(CLASS_VACANCY_SERVICE)
                .methodName(METHOD_CREATE_VACANCY)
                .userId(USER_ID_RECRUITER)
                .userEmail(EMAIL_RECRUITER)
                .userRole(ROLE_RECRUITER)
                .traceId(TRACE_ID_SAMPLE)
                .clientIp(CLIENT_IP_SAMPLE)
                .parameters(PARAMS_JSON_SAMPLE)
                .executionTimeMs(EXECUTION_TIME_MS)
                .success(true)
                .errorMessage(null)
                .exceptionClass(null)
                .build();
    }

    public static AuditLogEntity expectedFailedAuditEntity() {
        return AuditLogEntity.builder()
                .action(ACTION_CREATE_VACANCY)
                .className(CLASS_VACANCY_SERVICE)
                .methodName(METHOD_CREATE_VACANCY)
                .userId(USER_ID_RECRUITER)
                .userEmail(EMAIL_RECRUITER)
                .userRole(ROLE_RECRUITER)
                .traceId(TRACE_ID_SAMPLE)
                .clientIp(CLIENT_IP_SAMPLE)
                .parameters(PARAMS_JSON_SAMPLE)
                .executionTimeMs(EXECUTION_TIME_MS)
                .success(false)
                .errorMessage(ERROR_DUPLICATE_VACANCY)
                .exceptionClass(EXCEPTION_DUPLICATE_CLASS)
                .build();
    }

    public static AuditLogEntity expectedCandidateApplyEntity() {
        return AuditLogEntity.builder()
                .action(ACTION_APPLY_VACANCY)
                .className(CLASS_APPLICATION_SERVICE)
                .methodName(METHOD_APPLY)
                .userId(USER_ID_CANDIDATE)
                .userEmail(EMAIL_CANDIDATE)
                .userRole(ROLE_CANDIDATE)
                .traceId(TRACE_ID_SAMPLE)
                .clientIp(CLIENT_IP_SAMPLE)
                .parameters("{\"vacancyId\":\"b0000000-0000-0000-0000-000000000001\"}")
                .executionTimeMs(15L)
                .success(true)
                .errorMessage(null)
                .exceptionClass(null)
                .build();
    }
}
