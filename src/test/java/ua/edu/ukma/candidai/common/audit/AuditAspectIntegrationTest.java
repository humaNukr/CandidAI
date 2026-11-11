package ua.edu.ukma.candidai.common.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.common.annotation.IntegrationTest;
import ua.edu.ukma.candidai.common.audit.model.AuditLogEntity;
import ua.edu.ukma.candidai.common.exception.DuplicateResourceException;
import ua.edu.ukma.candidai.common.exception.InvalidStateTransitionException;
import ua.edu.ukma.candidai.common.logging.TraceIdFilter;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.request.ApplyForVacancyRequest;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;
import ua.edu.ukma.candidai.recruitment.service.ApplicationService;
import ua.edu.ukma.candidai.user.dto.request.CreateUserRequest;
import ua.edu.ukma.candidai.user.model.User;
import ua.edu.ukma.candidai.user.repository.UserRepository;
import ua.edu.ukma.candidai.user.service.UserService;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;
import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;
import ua.edu.ukma.candidai.vacancy.service.VacancyService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ua.edu.ukma.candidai.common.audit.AuditAspectTestResources.*;
import static ua.edu.ukma.candidai.testutils.IntegrationTestResources.*;

@IntegrationTest
@DisplayName("End-to-end integration tests for @AuditAction, BusinessAuditAspect and PostgreSQL audit_logs")
class AuditAspectIntegrationTest {

    @Autowired
    private VacancyService vacancyService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    @DisplayName("createVacancy - should persist audit_logs row with userId UUID, MDC context and JSON parameters")
    void givenAuthenticatedRecruiterWithMdc_createVacancy_shouldPersistSuccessAuditLogInPostgres() {
        authenticateSeedUser(SEED_RECRUITER_ID);
        setMdc("trace-recruiter-01", "192.168.10.25");
        CreateVacancyRequest request = aValidCreateVacancyRequest();

        VacancyResponse response = vacancyService.createVacancy(request);

        List<AuditLogEntity> auditLogs = findAuditLogsInDatabase("CREATE_VACANCY");
        assertThat(response).isNotNull();
        assertThat(auditLogs)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
                        "id", "createdAt", "executionTimeMs", "parameters"
                )
                .containsExactly(expectedCreateVacancySuccessAudit("trace-recruiter-01", "192.168.10.25"));
        AuditLogEntity persisted = auditLogs.getFirst();
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getExecutionTimeMs()).isGreaterThanOrEqualTo(0L);
        assertThat(persisted.getParameters()).contains(request.title());
    }

    @Test
    @DisplayName("createVacancy - should rethrow InvalidStateTransitionException, roll back skills and commit audit")
    void givenInvalidInitialStatus_createVacancy_shouldRethrowAndRollbackSkillsWhileCommittingFailedAudit() {
        authenticateSeedUser(SEED_RECRUITER_ID);
        setMdc("trace-rollback-01", "10.0.0.15");
        CreateVacancyRequest request = anInvalidInitialStatusVacancyRequest();

        assertThatThrownBy(() -> vacancyService.createVacancy(request))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessage(INVALID_INITIAL_STATUS_ERROR);

        Long skillCount = jdbcClient.sql("SELECT COUNT(*) FROM skills WHERE name = :name")
                .param("name", ROLLBACK_PROBE_SKILL)
                .query(Long.class)
                .single();
        Long vacancyCount = jdbcClient.sql("SELECT COUNT(*) FROM vacancies WHERE title = :title")
                .param("title", request.title())
                .query(Long.class)
                .single();
        List<AuditLogEntity> auditLogs = findAuditLogsInDatabase("CREATE_VACANCY");
        assertThat(skillCount).isZero();
        assertThat(vacancyCount).isZero();
        assertThat(auditLogs)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
                        "id", "createdAt", "executionTimeMs", "parameters"
                )
                .containsExactly(expectedCreateVacancyFailureAudit(
                        "trace-rollback-01",
                        "10.0.0.15",
                        InvalidStateTransitionException.class,
                        INVALID_INITIAL_STATUS_ERROR
                ));
    }

    @Test
    @DisplayName("updateVacancyStatus - should rethrow transition error, keep OPEN status and persist failed audit")
    void givenOpenVacancy_updateVacancyStatusToDraft_shouldRethrowAndKeepOpenStatusWithFailedAudit() {
        authenticateSeedUser(SEED_RECRUITER_ID);
        setMdc("trace-status-err-01", "10.0.0.22");
        VacancyResponse created = vacancyService.createVacancy(aValidCreateVacancyRequest());

        assertThatThrownBy(() -> vacancyService.updateVacancyStatus(
                created.id(),
                anInvalidDraftTransitionRequest()
        ))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessage(INVALID_TRANSITION_ERROR);

        String persistedStatus = jdbcClient.sql("SELECT status FROM vacancies WHERE id = :id")
                .param("id", created.id())
                .query(String.class)
                .single();
        List<AuditLogEntity> updateLogs = findAuditLogsInDatabase("UPDATE_VACANCY_STATUS");
        assertThat(persistedStatus).isEqualTo(VacancyStatus.OPEN.name());
        assertThat(updateLogs)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
                        "id", "createdAt", "executionTimeMs", "parameters"
                )
                .containsExactly(expectedUpdateVacancyStatusFailureAudit("trace-status-err-01", "10.0.0.22"));
    }

    @Test
    @DisplayName("POST /api/v1/vacancies - duplicate conflict should return 409 ProblemDetail and log both attempts")
    void givenDuplicateVacancyOverHttp_postVacancies_shouldReturn409ProblemDetailAndPersistBothAudits()
            throws Exception {
        User recruiter = loadSeedUser(SEED_RECRUITER_ID);
        CreateVacancyRequest request = aValidCreateVacancyRequest();
        String expectedDetail = expectedDuplicateVacancyMessage(request);

        mockMvc.perform(post(VACANCIES_URL)
                        .with(user(recruiter))
                        .header(TraceIdFilter.TRACE_ID_HEADER, "http-trace-ok-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        MvcResult conflictResult = mockMvc.perform(post(VACANCIES_URL)
                        .with(user(recruiter))
                        .header(TraceIdFilter.TRACE_ID_HEADER, "http-trace-dup-02")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andReturn();

        ProblemDetail actualProblem = parseProblemDetail(objectMapper, conflictResult);
        Long vacancyCount = jdbcClient.sql("SELECT COUNT(*) FROM vacancies WHERE title = :title")
                .param("title", request.title())
                .query(Long.class)
                .single();
        List<AuditLogEntity> auditLogs = findAuditLogsInDatabase("CREATE_VACANCY");
        assertThat(actualProblem)
                .usingRecursiveComparison()
                .ignoringFields("properties.timestamp")
                .isEqualTo(expectedConflictProblemDetail(expectedDetail));
        assertThat(vacancyCount).isEqualTo(1L);
        assertThat(auditLogs)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
                        "id", "createdAt", "executionTimeMs", "parameters"
                )
                .containsExactlyInAnyOrder(
                        expectedCreateVacancySuccessAudit("http-trace-ok-01", "127.0.0.1"),
                        expectedCreateVacancyFailureAudit(
                                "http-trace-dup-02",
                                "127.0.0.1",
                                DuplicateResourceException.class,
                                expectedDetail
                        )
                );
    }

    @Test
    @DisplayName("apply - should persist success audit and rethrow DuplicateResourceException with failed audit")
    void givenOpenVacancyAndDuplicateApplication_apply_shouldPersistBothAuditsWithoutDuplicateRow() {
        authenticateSeedUser(SEED_RECRUITER_ID);
        VacancyResponse createdVacancy = vacancyService.createVacancy(aValidCreateVacancyRequest());
        authenticateSeedUser(SEED_CANDIDATE_ID);
        setMdc("trace-apply-01", "172.16.0.5");
        ApplyForVacancyRequest applyRequest = aValidApplyRequest(createdVacancy.id());

        applicationService.apply(applyRequest);

        assertThatThrownBy(() -> applicationService.apply(applyRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage(expectedDuplicateApplyMessage(createdVacancy.id()));

        Long applicationCount = jdbcClient.sql("SELECT COUNT(*) FROM applications WHERE vacancy_id = :vacancyId")
                .param("vacancyId", createdVacancy.id())
                .query(Long.class)
                .single();
        List<AuditLogEntity> applyLogs = findAuditLogsInDatabase("APPLY_VACANCY");
        assertThat(applicationCount).isEqualTo(1L);
        assertThat(applyLogs)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
                        "id", "createdAt", "executionTimeMs", "parameters"
                )
                .containsExactlyInAnyOrder(
                        expectedApplyVacancySuccessAudit("trace-apply-01", "172.16.0.5"),
                        expectedApplyVacancyFailureAudit("trace-apply-01", "172.16.0.5", createdVacancy.id())
                );
    }

    @Test
    @DisplayName("createUser - should mask @AuditSensitive password field in PostgreSQL audit_logs")
    void givenSensitivePassword_createUser_shouldMaskPasswordInPostgresAuditLogs() {
        authenticateSeedUser(SEED_ADMIN_ID);
        setMdc("trace-user-01", "10.0.0.99");
        String email = "candidate.audit." + UUID.randomUUID().toString().substring(0, 8) + "@candidai.ua";
        CreateUserRequest request = aSensitiveCreateUserRequest(email);

        userService.createUser(request);

        List<AuditLogEntity> userLogs = findAuditLogsInDatabase("CREATE_USER");
        assertThat(userLogs)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
                        "id", "createdAt", "executionTimeMs", "parameters"
                )
                .containsExactly(expectedCreateUserSuccessAudit("trace-user-01", "10.0.0.99"));
        assertThat(userLogs.getFirst().getParameters())
                .contains("\"password\":\"" + MASKED_VALUE + "\"")
                .doesNotContain(SECRET_PASSWORD);
    }

    @Test
    @DisplayName("updateStatus overload - should route through ObjectProvider self-proxy and query audit_log view")
    void givenOverloadedUpdateStatusCall_updateStatus_shouldInterceptViaSelfProxyAndExposeAuditLogView() {
        authenticateSeedUser(SEED_RECRUITER_ID);
        VacancyResponse createdVacancy = vacancyService.createVacancy(aValidCreateVacancyRequest());
        authenticateSeedUser(SEED_CANDIDATE_ID);
        ApplicationResponse application = applicationService.apply(aValidApplyRequest(createdVacancy.id()));

        authenticateSeedUser(SEED_RECRUITER_ID);
        setMdc("trace-self-proxy-01", "10.0.0.77");
        applicationService.updateStatus(
                application.id(),
                ApplicationStatus.SCREENING,
                85,
                "Screening passed via self-injected proxy"
        );

        Long viewCount = jdbcClient.sql("""
                        SELECT COUNT(*)
                        FROM audit_log
                        WHERE action = 'UPDATE_APPLICATION_STATUS' AND trace_id = :traceId
                        """)
                .param("traceId", "trace-self-proxy-01")
                .query(Long.class)
                .single();
        assertThat(viewCount).isEqualTo(1L);
    }

    private User loadSeedUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow();
    }

    private void authenticateSeedUser(UUID userId) {
        User user = loadSeedUser(userId);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void setMdc(String traceId, String clientIp) {
        MDC.put(TraceIdFilter.TRACE_ID_MDC_KEY, traceId);
        MDC.put(TraceIdFilter.CLIENT_IP_MDC_KEY, clientIp);
    }

    private List<AuditLogEntity> findAuditLogsInDatabase(String action) {
        return jdbcClient.sql("""
                        SELECT id, action, class_name, method_name, user_id, user_email, user_role,
                               trace_id, client_ip, parameters, execution_time_ms, success,
                               error_message, exception_class, created_at
                        FROM audit_logs
                        WHERE action = :action
                        ORDER BY created_at DESC
                        """)
                .param("action", action)
                .query((rs, rowNum) -> AuditLogEntity.builder()
                        .id(rs.getObject("id", UUID.class))
                        .action(rs.getString("action"))
                        .className(rs.getString("class_name"))
                        .methodName(rs.getString("method_name"))
                        .userId(rs.getObject("user_id", UUID.class))
                        .userEmail(rs.getString("user_email"))
                        .userRole(rs.getString("user_role"))
                        .traceId(rs.getString("trace_id"))
                        .clientIp(rs.getString("client_ip"))
                        .parameters(rs.getString("parameters"))
                        .executionTimeMs(rs.getLong("execution_time_ms"))
                        .success(rs.getBoolean("success"))
                        .errorMessage(rs.getString("error_message"))
                        .exceptionClass(rs.getString("exception_class"))
                        .createdAt(rs.getTimestamp("created_at").toInstant())
                        .build())
                .list();
    }
}
