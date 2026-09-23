package ua.edu.ukma.candidai.assessment.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AiScreeningResult(
        UUID applicationId,
        UUID vacancyId,
        ScreeningStatus status,
        Integer matchingScore,
        Boolean passed,
        String summary,
        List<String> strengths,
        List<String> weaknesses,
        List<String> suggestedQuestions,
        String errorMessage,
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
