package ua.edu.ukma.candidai.common.audit.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.common.audit.annotation.AuditAction;
import ua.edu.ukma.candidai.common.audit.annotation.AuditSensitive;
import ua.edu.ukma.candidai.common.audit.model.AuditLogRecord;
import ua.edu.ukma.candidai.common.audit.service.AuditLogPersistenceService;
import ua.edu.ukma.candidai.common.logging.TraceIdFilter;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BusinessAuditAspectTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AuditLogPersistenceService persistenceService;
    private BusinessAuditAspect aspect;

    @BeforeEach
    void setUp() {
        persistenceService = mock(AuditLogPersistenceService.class);
        aspect = new BusinessAuditAspect(objectMapper, persistenceService);
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("auditOperation - should persist success audit log and return result when method succeeds")
    void givenSuccessfulExecution_auditOperation_shouldPersistSuccessAuditRecord() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        AuditAction auditAction = mock(AuditAction.class);

        Method sampleMethod = SampleTargetService.class.getMethod("executeAction", String.class);
        when(signature.getMethod()).thenReturn(sampleMethod);
        when(signature.getParameterNames()).thenReturn(new String[]{"targetId"});
        when(signature.getName()).thenReturn("executeAction");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new SampleTargetService());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"ID-100"});
        when(joinPoint.proceed()).thenReturn("SUCCESS_OUTPUT");

        when(auditAction.action()).thenReturn("TEST_ACTION");
        when(auditAction.logParameters()).thenReturn(true);

        MDC.put(TraceIdFilter.TRACE_ID_MDC_KEY, "trace-abc");
        MDC.put(TraceIdFilter.CLIENT_IP_MDC_KEY, "192.168.1.1");

        Object authPrincipal = new MockUserPrincipal(UUID.randomUUID(), "recruiter@candidai.com");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        authPrincipal, "credentials", List.of(new SimpleGrantedAuthority("ROLE_RECRUITER"))
                )
        );

        Object result = aspect.auditOperation(joinPoint, auditAction);

        assertThat(result).isEqualTo("SUCCESS_OUTPUT");

        ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
        verify(persistenceService).saveAuditLog(captor.capture());
        AuditLogRecord record = captor.getValue();

        assertThat(record.action()).isEqualTo("TEST_ACTION");
        assertThat(record.className()).isEqualTo("SampleTargetService");
        assertThat(record.methodName()).isEqualTo("executeAction");
        assertThat(record.userEmail()).isEqualTo("recruiter@candidai.com");
        assertThat(record.userRole()).isEqualTo("ROLE_RECRUITER");
        assertThat(record.traceId()).isEqualTo("trace-abc");
        assertThat(record.clientIp()).isEqualTo("192.168.1.1");
        assertThat(record.success()).isTrue();
        assertThat(record.errorMessage()).isNull();
        assertThat(record.exceptionClass()).isNull();
        assertThat(record.parameters()).contains("ID-100");
    }

    @Test
    @DisplayName("auditOperation - should persist failure audit log and rethrow original exception")
    void givenFailingExecution_auditOperation_shouldPersistFailureAndRethrow() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        AuditAction auditAction = mock(AuditAction.class);

        Method sampleMethod = SampleTargetService.class.getMethod("executeAction", String.class);
        when(signature.getMethod()).thenReturn(sampleMethod);
        when(signature.getParameterNames()).thenReturn(new String[]{"targetId"});
        when(signature.getName()).thenReturn("executeAction");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new SampleTargetService());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"ID-500"});
        when(joinPoint.proceed()).thenThrow(new IllegalArgumentException("Invalid target identifier"));

        when(auditAction.action()).thenReturn("FAIL_ACTION");
        when(auditAction.logParameters()).thenReturn(true);

        assertThatThrownBy(() -> aspect.auditOperation(joinPoint, auditAction))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid target identifier");

        ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
        verify(persistenceService).saveAuditLog(captor.capture());
        AuditLogRecord record = captor.getValue();

        assertThat(record.action()).isEqualTo("FAIL_ACTION");
        assertThat(record.success()).isFalse();
        assertThat(record.errorMessage()).isEqualTo("Invalid target identifier");
        assertThat(record.exceptionClass()).isEqualTo(IllegalArgumentException.class.getName());
    }

    @Test
    @DisplayName("auditOperation - should record ANONYMOUS actor when security context is absent")
    void givenNoAuthentication_auditOperation_shouldRecordAnonymousUser() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        AuditAction auditAction = mock(AuditAction.class);

        Method sampleMethod = SampleTargetService.class.getMethod("executeAction", String.class);
        when(signature.getMethod()).thenReturn(sampleMethod);
        when(signature.getParameterNames()).thenReturn(new String[]{"targetId"});
        when(signature.getName()).thenReturn("executeAction");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new SampleTargetService());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"ID-ANON"});
        when(joinPoint.proceed()).thenReturn("OK");

        when(auditAction.action()).thenReturn("PUBLIC_ACTION");
        when(auditAction.logParameters()).thenReturn(true);

        aspect.auditOperation(joinPoint, auditAction);

        ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
        verify(persistenceService).saveAuditLog(captor.capture());
        AuditLogRecord record = captor.getValue();

        assertThat(record.userId()).isNull();
        assertThat(record.userEmail()).isEqualTo("ANONYMOUS");
        assertThat(record.userRole()).isEqualTo("ROLE_ANONYMOUS");
    }

    @Test
    @DisplayName("auditOperation - should mask parameters and sensitive fields annotated with @AuditSensitive")
    void givenSensitiveData_auditOperation_shouldMaskSensitiveParametersAndFields() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        AuditAction auditAction = mock(AuditAction.class);

        Method sampleMethod = SampleTargetService.class.getMethod(
                "registerAccount", SampleUserDto.class, String.class
        );
        when(signature.getMethod()).thenReturn(sampleMethod);
        when(signature.getParameterNames()).thenReturn(new String[]{"dto", "rawSecret"});
        when(signature.getName()).thenReturn("registerAccount");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new SampleTargetService());

        SampleUserDto dto = new SampleUserDto("candidate@candidai.com", "PlainPassword123");
        when(joinPoint.getArgs()).thenReturn(new Object[]{dto, "SensitiveApiKey"});
        when(joinPoint.proceed()).thenReturn("CREATED");

        when(auditAction.action()).thenReturn("REGISTER_ACCOUNT");
        when(auditAction.logParameters()).thenReturn(true);

        aspect.auditOperation(joinPoint, auditAction);

        ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
        verify(persistenceService).saveAuditLog(captor.capture());
        AuditLogRecord record = captor.getValue();

        assertThat(record.parameters()).contains("candidate@candidai.com");
        assertThat(record.parameters()).contains("***MASKED***");
        assertThat(record.parameters()).doesNotContain("PlainPassword123");
        assertThat(record.parameters()).doesNotContain("SensitiveApiKey");
    }

    @Test
    @DisplayName("auditOperation - should leave parameters null when logParameters is false")
    void givenLogParametersFalse_auditOperation_shouldSetParametersToNull() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        AuditAction auditAction = mock(AuditAction.class);

        Method sampleMethod = SampleTargetService.class.getMethod("executeAction", String.class);
        when(signature.getMethod()).thenReturn(sampleMethod);
        when(signature.getName()).thenReturn("executeAction");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new SampleTargetService());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"ANY_ID"});
        when(joinPoint.proceed()).thenReturn("OK");

        when(auditAction.action()).thenReturn("QUIET_ACTION");
        when(auditAction.logParameters()).thenReturn(false);

        aspect.auditOperation(joinPoint, auditAction);

        ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
        verify(persistenceService).saveAuditLog(captor.capture());
        AuditLogRecord record = captor.getValue();

        assertThat(record.parameters()).isNull();
    }

    @Test
    @DisplayName("persistSafely - should not throw exception when persistence service fails")
    void givenFailingPersistence_auditOperation_shouldNotPropagatePersistenceException() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        AuditAction auditAction = mock(AuditAction.class);

        Method sampleMethod = SampleTargetService.class.getMethod("executeAction", String.class);
        when(signature.getMethod()).thenReturn(sampleMethod);
        when(signature.getName()).thenReturn("executeAction");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new SampleTargetService());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"ID"});
        when(joinPoint.proceed()).thenReturn("OUTPUT");

        when(auditAction.action()).thenReturn("PERSIST_FAIL");
        when(auditAction.logParameters()).thenReturn(false);

        doThrow(new RuntimeException("DB Connection Timeout"))
                .when(persistenceService).saveAuditLog(any(AuditLogRecord.class));

        Object result = aspect.auditOperation(joinPoint, auditAction);
        assertThat(result).isEqualTo("OUTPUT");
    }

    static class SampleTargetService {
        public String executeAction(String targetId) {
            return targetId;
        }

        public String registerAccount(SampleUserDto dto, @AuditSensitive String rawSecret) {
            return dto.email();
        }
    }

    record SampleUserDto(String email, @AuditSensitive String password) {}

    record MockUserPrincipal(UUID id, String email) {
        public UUID getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }
    }
}
