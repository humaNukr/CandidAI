package ua.edu.ukma.candidai.common.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import ua.edu.ukma.candidai.common.annotation.IntegrationTest;
import ua.edu.ukma.candidai.common.audit.model.AuditLogEntity;
import ua.edu.ukma.candidai.common.audit.repository.AuditLogRepository;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.common.logging.TraceIdFilter;
import ua.edu.ukma.candidai.user.UserRole;
import ua.edu.ukma.candidai.user.dto.request.CreateUserRequest;
import ua.edu.ukma.candidai.user.service.UserService;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;
import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;
import ua.edu.ukma.candidai.vacancy.service.VacancyService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ua.edu.ukma.candidai.testutils.IntegrationTestResources.aValidCreateVacancyRequest;

@IntegrationTest
@DisplayName("End-to-end integration tests for @AuditAction, BusinessAuditAspect and PostgreSQL persistence")
class AuditAspectIntegrationTest {

    private static final String TEST_RECRUITER_EMAIL = "recruiter.audit@candidai.com";

    @Autowired
    private VacancyService vacancyService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    @DisplayName("createVacancy - should trigger BusinessAuditAspect and persist successful audit log in PostgreSQL")
    void givenAuthenticatedRecruiter_whenCreateVacancy_thenAuditLogPersistedWithSuccess() {
        setupSecurityContext(TEST_RECRUITER_EMAIL, "ROLE_RECRUITER");
        MDC.put(TraceIdFilter.TRACE_ID_MDC_KEY, "trace-recruiter-01");
        MDC.put(TraceIdFilter.CLIENT_IP_MDC_KEY, "192.168.10.25");

        CreateVacancyRequest request = aValidCreateVacancyRequest();
        VacancyResponse response = vacancyService.createVacancy(request);

        assertThat(response).isNotNull();

        List<AuditLogEntity> auditLogs = auditLogRepository.findByActionOrderByCreatedAtDesc("CREATE_VACANCY");
        assertThat(auditLogs).isNotEmpty();

        AuditLogEntity log = auditLogs.getFirst();
        assertThat(log.getId()).isNotNull();
        assertThat(log.getAction()).isEqualTo("CREATE_VACANCY");
        assertThat(log.getClassName()).isEqualTo("VacancyServiceImpl");
        assertThat(log.getMethodName()).isEqualTo("createVacancy");
        assertThat(log.getUserEmail()).isEqualTo(TEST_RECRUITER_EMAIL);
        assertThat(log.getUserRole()).isEqualTo("ROLE_RECRUITER");
        assertThat(log.getTraceId()).isEqualTo("trace-recruiter-01");
        assertThat(log.getClientIp()).isEqualTo("192.168.10.25");
        assertThat(log.isSuccess()).isTrue();
        assertThat(log.getErrorMessage()).isNull();
        assertThat(log.getExceptionClass()).isNull();
        assertThat(log.getExecutionTimeMs()).isNotNull().isGreaterThanOrEqualTo(0L);
        assertThat(log.getParameters()).contains(request.title());
    }

    @Test
    @DisplayName("deleteVacancy - should rethrow exception and persist failed audit log with error details")
    void givenFailingOperation_whenDeleteNonExistentVacancy_thenExceptionRethrownAndFailedAuditPersisted() {
        setupSecurityContext("admin.audit@candidai.com", "ROLE_ADMIN");
        UUID nonExistentId = UUID.randomUUID();

        assertThatThrownBy(() -> vacancyService.deleteVacancy(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(nonExistentId.toString());

        List<AuditLogEntity> failedLogs = auditLogRepository.findByActionOrderByCreatedAtDesc("DELETE_VACANCY");
        assertThat(failedLogs).isNotEmpty();

        AuditLogEntity log = failedLogs.getFirst();
        assertThat(log.getId()).isNotNull();
        assertThat(log.getAction()).isEqualTo("DELETE_VACANCY");
        assertThat(log.getClassName()).isEqualTo("VacancyServiceImpl");
        assertThat(log.getMethodName()).isEqualTo("deleteVacancy");
        assertThat(log.getUserEmail()).isEqualTo("admin.audit@candidai.com");
        assertThat(log.getUserRole()).isEqualTo("ROLE_ADMIN");
        assertThat(log.isSuccess()).isFalse();
        assertThat(log.getErrorMessage()).contains(nonExistentId.toString());
        assertThat(log.getExceptionClass()).isEqualTo(ResourceNotFoundException.class.getName());
        assertThat(log.getExecutionTimeMs()).isNotNull().isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("createUser - should mask @AuditSensitive fields and persist sanitized parameters in audit_logs")
    void givenSensitivePayload_whenCreateUser_thenPasswordIsMaskedInAuditLogs() {
        setupSecurityContext("admin.audit@candidai.com", "ROLE_ADMIN");
        String secretPassword = "SuperSecretPassword123!";
        CreateUserRequest request = new CreateUserRequest(
                "Audit Test Candidate",
                "candidate.audit." + UUID.randomUUID().toString().substring(0, 8) + "@candidai.com",
                secretPassword,
                UserRole.CANDIDATE,
                "123456789"
        );

        userService.createUser(request);

        List<AuditLogEntity> userLogs = auditLogRepository.findByActionOrderByCreatedAtDesc("CREATE_USER");
        assertThat(userLogs).isNotEmpty();

        AuditLogEntity log = userLogs.getFirst();
        assertThat(log.isSuccess()).isTrue();
        assertThat(log.getParameters()).contains("***MASKED***");
        assertThat(log.getParameters()).doesNotContain(secretPassword);
    }

    private void setupSecurityContext(String email, String role) {
        UserDetails userDetails = User.withUsername(email)
                .password("unused")
                .authorities(new SimpleGrantedAuthority(role))
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
