package ua.edu.ukma.candidai.common.audit;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.common.audit.model.AuditLogEntity;
import ua.edu.ukma.candidai.common.exception.DuplicateResourceException;
import ua.edu.ukma.candidai.common.exception.InvalidStateTransitionException;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.model.InterviewDecision;
import ua.edu.ukma.candidai.recruitment.dto.request.SubmitInterviewFeedbackRequest;
import ua.edu.ukma.candidai.recruitment.dto.request.UpdateApplicationStatusRequest;
import ua.edu.ukma.candidai.user.UserRole;
import ua.edu.ukma.candidai.user.dto.request.CreateUserRequest;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;
import ua.edu.ukma.candidai.vacancy.dto.request.UpdateVacancyStatusRequest;
import ua.edu.ukma.candidai.vacancy.model.EmploymentType;
import ua.edu.ukma.candidai.vacancy.model.EnglishLevel;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.LocationType;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static ua.edu.ukma.candidai.testutils.IntegrationTestResources.*;

public final class AuditAspectTestResources {

    public static final String VACANCIES_URL = "/api/v1/vacancies";
    public static final String RECRUITER_EMAIL = "author@candidai.ua";
    public static final String CANDIDATE_EMAIL = "candidate@candidai.ua";
    public static final String ADMIN_EMAIL = "admin@candidai.ua";
    public static final String ROLE_RECRUITER = "ROLE_RECRUITER";
    public static final String ROLE_CANDIDATE = "ROLE_CANDIDATE";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLLBACK_PROBE_SKILL = "QuantumAuditSkill";
    public static final String MASKED_VALUE = "***MASKED***";
    public static final String SECRET_PASSWORD = "SuperSecretPassword123!";
    public static final String INVALID_INITIAL_STATUS_ERROR
            = "Initial vacancy status must be DRAFT or OPEN, got: CLOSED";
    public static final String INVALID_TRANSITION_ERROR
            = "Cannot transition vacancy status from OPEN to DRAFT";

    private AuditAspectTestResources() {
    }

    public static CreateVacancyRequest anInvalidInitialStatusVacancyRequest() {
        return new CreateVacancyRequest(
                SEED_RECRUITER_ID,
                SEED_RECRUITER_ID,
                SEED_COMPANY_ID,
                VacancyStatus.CLOSED,
                "Audit Rollback Verification Role",
                JobCategory.ENGINEERING,
                "Platform Engineering",
                "Senior",
                5,
                "Valid description used to verify transactional rollback with REQUIRES_NEW audit.",
                List.of(ROLLBACK_PROBE_SKILL),
                List.of(),
                EnglishLevel.B2,
                new BigDecimal("4000.00"),
                new BigDecimal("6000.00"),
                "USD",
                EmploymentType.FULL_TIME,
                LocationType.REMOTE,
                "Kyiv, Ukraine",
                Instant.now().plus(Duration.ofDays(30))
        );
    }

    public static UpdateVacancyStatusRequest anInvalidDraftTransitionRequest() {
        return new UpdateVacancyStatusRequest(VacancyStatus.DRAFT);
    }

    public static CreateUserRequest aSensitiveCreateUserRequest(String email) {
        return new CreateUserRequest(
                "Audit Test Candidate",
                email,
                SECRET_PASSWORD,
                UserRole.CANDIDATE,
                "123456789"
        );
    }

    public static String expectedDuplicateVacancyMessage(CreateVacancyRequest request) {
        return "Active vacancy with title '" + request.title() + "' already exists for author "
                + request.authorId();
    }

    public static String expectedDuplicateApplyMessage(UUID vacancyId) {
        return "Candidate with email candidate@candidai.ua has already applied to vacancy " + vacancyId;
    }

    public static AuditLogEntity expectedCreateVacancySuccessAudit(String traceId, String clientIp) {
        return AuditLogEntity.builder()
                .action("CREATE_VACANCY")
                .className("VacancyServiceImpl")
                .methodName("createVacancy")
                .userId(SEED_RECRUITER_ID)
                .userEmail(RECRUITER_EMAIL)
                .userRole(ROLE_RECRUITER)
                .traceId(traceId)
                .clientIp(clientIp)
                .success(true)
                .errorMessage(null)
                .exceptionClass(null)
                .build();
    }

    public static AuditLogEntity expectedCreateVacancyFailureAudit(
            String traceId,
            String clientIp,
            Class<? extends Throwable> exceptionClass,
            String errorMessage
    ) {
        return AuditLogEntity.builder()
                .action("CREATE_VACANCY")
                .className("VacancyServiceImpl")
                .methodName("createVacancy")
                .userId(SEED_RECRUITER_ID)
                .userEmail(RECRUITER_EMAIL)
                .userRole(ROLE_RECRUITER)
                .traceId(traceId)
                .clientIp(clientIp)
                .success(false)
                .errorMessage(errorMessage)
                .exceptionClass(exceptionClass.getName())
                .build();
    }

    public static AuditLogEntity expectedUpdateVacancyStatusFailureAudit(String traceId, String clientIp) {
        return AuditLogEntity.builder()
                .action("UPDATE_VACANCY_STATUS")
                .className("VacancyServiceImpl")
                .methodName("updateVacancyStatus")
                .userId(SEED_RECRUITER_ID)
                .userEmail(RECRUITER_EMAIL)
                .userRole(ROLE_RECRUITER)
                .traceId(traceId)
                .clientIp(clientIp)
                .success(false)
                .errorMessage(INVALID_TRANSITION_ERROR)
                .exceptionClass(InvalidStateTransitionException.class.getName())
                .build();
    }

    public static AuditLogEntity expectedApplyVacancySuccessAudit(String traceId, String clientIp) {
        return AuditLogEntity.builder()
                .action("APPLY_VACANCY")
                .className("ApplicationServiceImpl")
                .methodName("apply")
                .userId(SEED_CANDIDATE_ID)
                .userEmail(CANDIDATE_EMAIL)
                .userRole(ROLE_CANDIDATE)
                .traceId(traceId)
                .clientIp(clientIp)
                .success(true)
                .errorMessage(null)
                .exceptionClass(null)
                .build();
    }

    public static AuditLogEntity expectedApplyVacancyFailureAudit(
            String traceId,
            String clientIp,
            UUID vacancyId
    ) {
        return AuditLogEntity.builder()
                .action("APPLY_VACANCY")
                .className("ApplicationServiceImpl")
                .methodName("apply")
                .userId(SEED_CANDIDATE_ID)
                .userEmail(CANDIDATE_EMAIL)
                .userRole(ROLE_CANDIDATE)
                .traceId(traceId)
                .clientIp(clientIp)
                .success(false)
                .errorMessage(expectedDuplicateApplyMessage(vacancyId))
                .exceptionClass(DuplicateResourceException.class.getName())
                .build();
    }

    public static SubmitInterviewFeedbackRequest aHireFeedbackRequest() {
        return new SubmitInterviewFeedbackRequest(
                "Tech Lead",
                5,
                "Strong Java and Spring knowledge",
                InterviewDecision.HIRE
        );
    }

    public static UpdateApplicationStatusRequest anOfferStatusRequest() {
        return new UpdateApplicationStatusRequest(
                ApplicationStatus.OFFER,
                "Recommended for offer"
        );
    }

    public static AuditLogEntity expectedUpdateApplicationStatusSuccessAudit(String traceId, String clientIp) {
        return AuditLogEntity.builder()
                .action("UPDATE_APPLICATION_STATUS")
                .className("ApplicationServiceImpl")
                .methodName("updateStatus")
                .userId(SEED_RECRUITER_ID)
                .userEmail(RECRUITER_EMAIL)
                .userRole(ROLE_RECRUITER)
                .traceId(traceId)
                .clientIp(clientIp)
                .success(true)
                .errorMessage(null)
                .exceptionClass(null)
                .build();
    }

    public static AuditLogEntity expectedEvaluateApplicationSuccessAudit(String traceId, String clientIp) {
        return AuditLogEntity.builder()
                .action("EVALUATE_APPLICATION")
                .className("CandidateEvaluationServiceImpl")
                .methodName("evaluateCandidate")
                .userId(SEED_RECRUITER_ID)
                .userEmail(RECRUITER_EMAIL)
                .userRole(ROLE_RECRUITER)
                .traceId(traceId)
                .clientIp(clientIp)
                .success(true)
                .errorMessage(null)
                .exceptionClass(null)
                .build();
    }

    public static AuditLogEntity expectedCreateUserSuccessAudit(String traceId, String clientIp) {
        return AuditLogEntity.builder()
                .action("CREATE_USER")
                .className("UserServiceImpl")
                .methodName("createUser")
                .userId(SEED_ADMIN_ID)
                .userEmail(ADMIN_EMAIL)
                .userRole(ROLE_ADMIN)
                .traceId(traceId)
                .clientIp(clientIp)
                .success(true)
                .errorMessage(null)
                .exceptionClass(null)
                .build();
    }

    public static ProblemDetail expectedConflictProblemDetail(String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
        problemDetail.setTitle("Resource Conflict");
        problemDetail.setType(URI.create("https://candidai.ukma.edu.ua/errors/conflict"));
        problemDetail.setInstance(URI.create(VACANCIES_URL));
        problemDetail.setProperty("timestamp", Instant.EPOCH);
        return problemDetail;
    }

    public static ProblemDetail parseProblemDetail(ObjectMapper objectMapper, MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), ProblemDetail.class);
    }
}
