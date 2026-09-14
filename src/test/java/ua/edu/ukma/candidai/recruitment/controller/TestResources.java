package ua.edu.ukma.candidai.recruitment.controller;

import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.model.InterviewDecision;
import ua.edu.ukma.candidai.recruitment.dto.request.SubmitInterviewFeedbackRequest;
import ua.edu.ukma.candidai.recruitment.dto.request.UpdateApplicationStatusRequest;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;

import java.time.Instant;
import java.util.UUID;

class TestResources {

    static final String BASE_URL = "/api/v1/applications";
    static final UUID DEFAULT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID DEFAULT_VACANCY_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    static final UUID NON_EXISTENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    static final String JSON_WITH_UNKNOWN_PROPERTY = """
            {
                "unknownField": "bad"
            }
            """;

    static final String VALIDATION_ERROR_JSON = """
            {
                "type": "https://candidai.ukma.edu.ua/errors/validation",
                "title": "Validation Error",
                "status": 400,
                "detail": "Input validation failed"
            }
            """;

    static final String JSON_PARSING_ERROR_JSON = """
            {
                "type": "https://candidai.ukma.edu.ua/errors/bad-request",
                "title": "JSON Parsing Error",
                "status": 400,
                "detail": "Malformed request body or unknown properties"
            }
            """;

    static String notFoundProblemDetailJson(UUID id) {
        return """
                {
                    "type": "https://candidai.ukma.edu.ua/errors/not-found",
                    "title": "Resource Not Found",
                    "status": 404,
                    "detail": "Application not found with id: %s"
                }
                """.formatted(id);
    }

    static UpdateApplicationStatusRequest validUpdateStatusRequest() {
        return new UpdateApplicationStatusRequest(
                ApplicationStatus.INTERVIEW,
                "Candidate passed screening successfully"
        );
    }

    static SubmitInterviewFeedbackRequest validSubmitFeedbackRequest() {
        return new SubmitInterviewFeedbackRequest(
                "Alex Techlead",
                4,
                "Strong knowledge of Java and Spring Boot architecture",
                InterviewDecision.HIRE
        );
    }

    static ApplicationResponse anApplicationResponse() {
        return new ApplicationResponse(
                DEFAULT_ID,
                DEFAULT_VACANCY_ID,
                "John Doe",
                "john.doe@example.com",
                "+380501234567",
                "https://storage.candidai.ukma.edu.ua/resumes/john_doe.pdf",
                ApplicationStatus.APPLIED,
                null,
                Instant.parse("2026-09-12T10:00:00Z"),
                Instant.parse("2026-09-12T10:00:00Z")
        );
    }
}
