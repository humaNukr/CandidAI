package ua.edu.ukma.candidai.recruitment;

import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.model.InterviewDecision;
import ua.edu.ukma.candidai.recruitment.dto.request.ApplyForVacancyRequest;
import ua.edu.ukma.candidai.recruitment.dto.request.SubmitInterviewFeedbackRequest;
import ua.edu.ukma.candidai.recruitment.dto.request.UpdateApplicationStatusRequest;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;
import ua.edu.ukma.candidai.recruitment.dto.response.InterviewFeedbackResponse;
import ua.edu.ukma.candidai.recruitment.model.Application;
import ua.edu.ukma.candidai.recruitment.service.strategy.EvaluationResult;

import java.time.Instant;
import java.util.UUID;

public final class RecruitmentTestResources {

    public static final String BASE_URL = "/api/v1/applications";
    public static final UUID DEFAULT_APPLICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID DEFAULT_ID = DEFAULT_APPLICATION_ID;
    public static final UUID DEFAULT_VACANCY_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    public static final UUID DEFAULT_CANDIDATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID DEFAULT_FEEDBACK_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    public static final UUID NON_EXISTENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    public static final String DEFAULT_CANDIDATE_NAME = "John Doe";
    public static final String DEFAULT_EMAIL = "john.doe@example.com";
    public static final String DEFAULT_PHONE = "+380501234567";
    public static final String DEFAULT_RESUME_URL = "https://storage.candidai.ukma.edu.ua/resumes/john_doe.pdf";
    public static final Instant DEFAULT_NOW = Instant.parse("2026-09-20T10:00:00Z");

    public static final String JSON_WITH_UNKNOWN_PROPERTY = """
            {
                "unknownField": "bad"
            }
            """;

    public static final String VALIDATION_ERROR_JSON = """
            {
                "type": "https://candidai.ukma.edu.ua/errors/validation",
                "title": "Validation Error",
                "status": 400,
                "detail": "Input validation failed"
            }
            """;

    public static final String JSON_PARSING_ERROR_JSON = """
            {
                "type": "https://candidai.ukma.edu.ua/errors/bad-request",
                "title": "JSON Parsing Error",
                "status": 400,
                "detail": "Malformed request body or unknown properties"
            }
            """;

    private RecruitmentTestResources() {
    }

    public static String notFoundProblemDetailJson(UUID id) {
        return """
                {
                    "type": "https://candidai.ukma.edu.ua/errors/not-found",
                    "title": "Resource Not Found",
                    "status": 404,
                    "detail": "Application not found with id: %s"
                }
                """.formatted(id);
    }

    public static ApplyForVacancyRequestBuilder anApplyRequest() {
        return new ApplyForVacancyRequestBuilder();
    }

    public static ApplyForVacancyRequest validApplyRequest() {
        return anApplyRequest().build();
    }

    public static UpdateApplicationStatusRequestBuilder aUpdateStatusRequest() {
        return new UpdateApplicationStatusRequestBuilder();
    }

    public static SubmitInterviewFeedbackRequestBuilder aSubmitFeedbackRequest() {
        return new SubmitInterviewFeedbackRequestBuilder();
    }

    public static ApplyForVacancyRequestBuilder anApplyForVacancyRequest() {
        return new ApplyForVacancyRequestBuilder();
    }

    public static ApplyForVacancyRequest validApplyForVacancyRequest() {
        return anApplyForVacancyRequest().build();
    }

    public static ApplyForVacancyRequest sampleApplyRequest() {
        return validApplyForVacancyRequest();
    }

    public static ApplyForVacancyRequest sampleApplyRequestWithCandidateId(UUID candidateId) {
        return anApplyForVacancyRequest().candidateId(candidateId).build();
    }

    public static Application.ApplicationBuilder anApplicationBuilder() {
        return Application.builder()
                .id(DEFAULT_APPLICATION_ID)
                .vacancyId(DEFAULT_VACANCY_ID)
                .candidateId(DEFAULT_CANDIDATE_ID)
                .candidateName(DEFAULT_CANDIDATE_NAME)
                .email(DEFAULT_EMAIL)
                .phone(DEFAULT_PHONE)
                .resumeUrl(DEFAULT_RESUME_URL)
                .status(ApplicationStatus.APPLIED)
                .comment(null)
                .matchingScore(null)
                .appliedAt(DEFAULT_NOW)
                .updatedAt(DEFAULT_NOW);
    }

    public static Application anApplication() {
        return anApplicationBuilder().build();
    }

    public static Application anApplication(ApplicationStatus status) {
        return anApplicationBuilder().status(status).build();
    }

    public static Application anApplication(ApplicationStatus status, UUID candidateId) {
        return anApplicationBuilder().status(status).candidateId(candidateId).build();
    }

    public static Application anApplication(ApplicationStatus status, Instant updatedAt) {
        return anApplicationBuilder().status(status).updatedAt(updatedAt).build();
    }

    public static Application anApplication(
            ApplicationStatus status,
            Integer matchingScore,
            String comment,
            Instant updatedAt
    ) {
        return anApplicationBuilder()
                .status(status)
                .matchingScore(matchingScore)
                .comment(comment)
                .updatedAt(updatedAt)
                .build();
    }

    public static Application sampleApplication() {
        return anApplication();
    }

    public static Application sampleApplication(UUID candidateId) {
        return anApplication(ApplicationStatus.APPLIED, candidateId);
    }

    public static Application sampleApplication(ApplicationStatus status) {
        return anApplication(status);
    }

    public static Application sampleApplication(ApplicationStatus status, UUID candidateId) {
        return anApplication(status, candidateId);
    }

    public static ApplicationResponse anApplicationResponse() {
        return anApplicationResponse(ApplicationStatus.APPLIED, DEFAULT_NOW);
    }

    public static ApplicationResponse anApplicationResponse(ApplicationStatus status) {
        return anApplicationResponse(status, DEFAULT_NOW);
    }

    public static ApplicationResponse anApplicationResponse(ApplicationStatus status, Instant updatedAt) {
        return anApplicationResponse(status, null, null, updatedAt);
    }

    public static ApplicationResponse anApplicationResponse(ApplicationStatus status, String comment) {
        return anApplicationResponse(status, null, comment, DEFAULT_NOW);
    }

    public static ApplicationResponse anApplicationResponse(
            ApplicationStatus status,
            Integer matchingScore,
            String comment,
            Instant updatedAt
    ) {
        return new ApplicationResponse(
                DEFAULT_APPLICATION_ID,
                DEFAULT_VACANCY_ID,
                DEFAULT_CANDIDATE_ID,
                DEFAULT_CANDIDATE_NAME,
                DEFAULT_EMAIL,
                DEFAULT_PHONE,
                DEFAULT_RESUME_URL,
                status,
                matchingScore,
                comment,
                DEFAULT_NOW,
                updatedAt
        );
    }

    public static ApplicationResponse updatedApplicationResponse() {
        return anApplicationResponse(ApplicationStatus.INTERVIEW, "Candidate passed screening successfully");
    }

    public static ApplicationResponse sampleApplicationResponse() {
        return anApplicationResponse();
    }

    public static ApplicationResponse sampleApplicationResponse(UUID candidateId) {
        return new ApplicationResponse(
                DEFAULT_APPLICATION_ID,
                DEFAULT_VACANCY_ID,
                candidateId,
                DEFAULT_CANDIDATE_NAME,
                DEFAULT_EMAIL,
                DEFAULT_PHONE,
                DEFAULT_RESUME_URL,
                ApplicationStatus.APPLIED,
                null,
                null,
                DEFAULT_NOW,
                DEFAULT_NOW
        );
    }

    public static ApplicationSubmittedEvent sampleApplicationSubmittedEvent() {
        return sampleApplicationSubmittedEvent(DEFAULT_CANDIDATE_ID);
    }

    public static ApplicationSubmittedEvent sampleApplicationSubmittedEvent(UUID candidateId) {
        return new ApplicationSubmittedEvent(
                DEFAULT_APPLICATION_ID,
                DEFAULT_VACANCY_ID,
                candidateId,
                DEFAULT_CANDIDATE_NAME,
                DEFAULT_EMAIL,
                DEFAULT_RESUME_URL,
                DEFAULT_NOW
        );
    }

    public static ApplicationStatusChangedEvent sampleApplicationStatusChangedEvent() {
        return sampleApplicationStatusChangedEvent(DEFAULT_CANDIDATE_ID);
    }

    public static ApplicationStatusChangedEvent sampleApplicationStatusChangedEvent(UUID candidateId) {
        return new ApplicationStatusChangedEvent(
                DEFAULT_APPLICATION_ID,
                DEFAULT_VACANCY_ID,
                candidateId,
                DEFAULT_CANDIDATE_NAME,
                DEFAULT_EMAIL,
                ApplicationStatus.APPLIED,
                ApplicationStatus.SCREENING,
                "Passed resume screening",
                DEFAULT_NOW
        );
    }

    public static UpdateApplicationStatusRequest validUpdateStatusRequest() {
        return new UpdateApplicationStatusRequest(
                ApplicationStatus.SCREENING,
                "Passed screening"
        );
    }

    public static SubmitInterviewFeedbackRequest validSubmitFeedbackRequest() {
        return new SubmitInterviewFeedbackRequest(
                "Alex Lead",
                4,
                "Strong skills",
                InterviewDecision.HIRE
        );
    }

    public static InterviewFeedbackResponse aFeedbackResponse() {
        return aFeedbackResponse(4);
    }

    public static InterviewFeedbackResponse aFeedbackResponse(Integer score) {
        return aFeedbackResponse(DEFAULT_FEEDBACK_ID, "Alex Lead", score, "Strong skills");
    }

    public static InterviewFeedbackResponse aFeedbackResponse(
            UUID id,
            String interviewerName,
            Integer score,
            String notes
    ) {
        return aFeedbackResponse(id, interviewerName, score, notes, InterviewDecision.HIRE);
    }

    public static InterviewFeedbackResponse aFeedbackResponse(
            UUID id,
            String interviewerName,
            Integer score,
            String notes,
            InterviewDecision decision
    ) {
        return new InterviewFeedbackResponse(
                id,
                DEFAULT_APPLICATION_ID,
                interviewerName,
                score,
                notes,
                decision,
                DEFAULT_NOW
        );
    }

    public static InterviewFeedbackResponse aRejectFeedbackResponse() {
        return aFeedbackResponse(DEFAULT_FEEDBACK_ID, "Lead", 2, "Poor", InterviewDecision.REJECT);
    }

    public static InterviewFeedbackResponse aHireFeedbackResponse() {
        return aFeedbackResponse(DEFAULT_FEEDBACK_ID, "Lead", 5, "Great", InterviewDecision.HIRE);
    }

    public static InterviewFeedbackResponse anInterviewFeedbackResponse() {
        return anInterviewFeedbackResponse("Strong knowledge of Java and Spring Boot architecture");
    }

    public static InterviewFeedbackResponse anInterviewFeedbackResponse(String notes) {
        return aFeedbackResponse(DEFAULT_FEEDBACK_ID, "Alex Techlead", 4, notes);
    }

    public static EvaluationResult anEvaluationResult() {
        return new EvaluationResult(
                4.5,
                InterviewDecision.HIRE,
                "Engineering evaluation completed"
        );
    }

    public static EvaluationResult anEvaluationResult(InterviewDecision decision) {
        return new EvaluationResult(
                decision == InterviewDecision.HIRE ? 4.5 : 2.0,
                decision,
                decision == InterviewDecision.HIRE
                        ? "Engineering evaluation completed"
                        : "Candidate evaluation failed"
        );
    }

    public static class ApplyForVacancyRequestBuilder {
        private UUID vacancyId = DEFAULT_VACANCY_ID;
        private UUID candidateId = DEFAULT_CANDIDATE_ID;
        private String candidateName = DEFAULT_CANDIDATE_NAME;
        private String email = DEFAULT_EMAIL;
        private String phone = DEFAULT_PHONE;
        private String resumeUrl = DEFAULT_RESUME_URL;

        public ApplyForVacancyRequestBuilder vacancyId(UUID vacancyId) {
            this.vacancyId = vacancyId;
            return this;
        }

        public ApplyForVacancyRequestBuilder candidateId(UUID candidateId) {
            this.candidateId = candidateId;
            return this;
        }

        public ApplyForVacancyRequestBuilder candidateName(String candidateName) {
            this.candidateName = candidateName;
            return this;
        }

        public ApplyForVacancyRequestBuilder email(String email) {
            this.email = email;
            return this;
        }

        public ApplyForVacancyRequestBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public ApplyForVacancyRequestBuilder resumeUrl(String resumeUrl) {
            this.resumeUrl = resumeUrl;
            return this;
        }

        public ApplyForVacancyRequest build() {
            return new ApplyForVacancyRequest(
                    vacancyId,
                    candidateId,
                    candidateName,
                    email,
                    phone,
                    resumeUrl
            );
        }
    }

    public static class UpdateApplicationStatusRequestBuilder {
        private ApplicationStatus status = ApplicationStatus.INTERVIEW;
        private String comment = "Candidate passed screening successfully";

        public UpdateApplicationStatusRequestBuilder status(ApplicationStatus status) {
            this.status = status;
            return this;
        }

        public UpdateApplicationStatusRequestBuilder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public UpdateApplicationStatusRequest build() {
            return new UpdateApplicationStatusRequest(status, comment);
        }
    }

    public static class SubmitInterviewFeedbackRequestBuilder {
        private String interviewerName = "Alex Techlead";
        private Integer technicalScore = 4;
        private String notes = "Strong knowledge of Java and Spring Boot architecture";
        private InterviewDecision decision = InterviewDecision.HIRE;

        public SubmitInterviewFeedbackRequestBuilder interviewerName(String interviewerName) {
            this.interviewerName = interviewerName;
            return this;
        }

        public SubmitInterviewFeedbackRequestBuilder technicalScore(Integer technicalScore) {
            this.technicalScore = technicalScore;
            return this;
        }

        public SubmitInterviewFeedbackRequestBuilder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public SubmitInterviewFeedbackRequestBuilder decision(InterviewDecision decision) {
            this.decision = decision;
            return this;
        }

        public SubmitInterviewFeedbackRequest build() {
            return new SubmitInterviewFeedbackRequest(interviewerName, technicalScore, notes, decision);
        }
    }
}
