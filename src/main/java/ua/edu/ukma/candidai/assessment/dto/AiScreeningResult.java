package ua.edu.ukma.candidai.assessment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "AI candidate screening assessment result")
public record AiScreeningResult(
        @Schema(description = "Associated application ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID applicationId,

        @Schema(description = "Target vacancy ID", example = "a0000000-0000-0000-0000-000000000001")
        UUID vacancyId,

        @Schema(description = "Screening processing status", example = "COMPLETED")
        ScreeningStatus status,

        @Schema(description = "Matching score (0 to 100)", example = "87")
        Integer matchingScore,

        @Schema(description = "Whether the candidate passed AI screening", example = "true")
        Boolean passed,

        @Schema(description = "Executive evaluation summary", example = "Strong match for Senior Java role.")
        String summary,

        @Schema(
                description = "Identified candidate strengths",
                example = "[\"Spring Boot\", \"Modulith\", \"Clean Code\"]"
        )
        List<String> strengths,

        @Schema(description = "Identified candidate weaknesses", example = "[\"Limited Kubernetes experience\"]")
        List<String> weaknesses,

        @Schema(
                description = "Recommended questions for technical interview",
                example = "[\"Explain transaction boundaries in Spring Modulith\"]"
        )
        List<String> suggestedQuestions,

        @Schema(description = "Error message if screening failed", example = "null")
        String errorMessage,

        @Schema(description = "Evaluation timestamp", example = "2026-10-05T08:30:00Z")
        Instant evaluatedAt
) {
    public static AiScreeningResult completed(
            UUID applicationId,
            UUID vacancyId,
            int matchingScore,
            boolean passed,
            String summary,
            List<String> strengths,
            List<String> weaknesses,
            List<String> suggestedQuestions,
            Instant evaluatedAt
    ) {
        return new AiScreeningResult(
                applicationId,
                vacancyId,
                ScreeningStatus.COMPLETED,
                matchingScore,
                passed,
                summary,
                strengths != null ? strengths : List.of(),
                weaknesses != null ? weaknesses : List.of(),
                suggestedQuestions != null ? suggestedQuestions : List.of(),
                null,
                evaluatedAt
        );
    }

    public static AiScreeningResult failed(
            UUID applicationId,
            UUID vacancyId,
            String errorMessage,
            Instant evaluatedAt
    ) {
        return new AiScreeningResult(
                applicationId,
                vacancyId,
                ScreeningStatus.FAILED,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                errorMessage,
                evaluatedAt
        );
    }
}
