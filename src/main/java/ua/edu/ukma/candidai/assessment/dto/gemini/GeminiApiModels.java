package ua.edu.ukma.candidai.assessment.dto.gemini;

import java.util.List;

public final class GeminiApiModels {

    private static final double DEFAULT_TEMPERATURE = 0.2;

    private GeminiApiModels() {
    }

    public record GenerateContentRequest(
            List<Content> contents,
            GenerationConfig generationConfig
    ) {
        public static GenerateContentRequest of(String prompt) {
            return new GenerateContentRequest(
                    List.of(new Content(List.of(new Part(prompt)))),
                    new GenerationConfig("application/json", DEFAULT_TEMPERATURE)
            );
        }
    }

    public record Content(List<Part> parts) {}

    public record Part(String text) {}

    public record GenerationConfig(
            String responseMimeType,
            Double temperature
    ) {}

    public record GenerateContentResponse(
            List<Candidate> candidates
    ) {
        public String extractText() {
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }
            Candidate first = candidates.getFirst();
            if (first.content() == null || first.content().parts() == null || first.content().parts().isEmpty()) {
                return null;
            }
            return first.content().parts().getFirst().text();
        }
    }

    public record Candidate(Content content) {}

    public record GeminiScreeningPayload(
            Integer matchingScore,
            Boolean passed,
            String summary,
            List<String> strengths,
            List<String> weaknesses,
            List<String> suggestedQuestions
    ) {
        private static final int MIN_SCORE = 0;
        private static final int MAX_SCORE = 100;

        public void validate() {
            if (matchingScore == null || matchingScore < MIN_SCORE || matchingScore > MAX_SCORE) {
                throw new IllegalStateException("Matching score must be between 0 and 100: " + matchingScore);
            }
            if (passed == null) {
                throw new IllegalStateException("Passed flag must not be null");
            }
            if (summary == null || summary.isBlank()) {
                throw new IllegalStateException("Summary must not be blank");
            }
        }
    }
}
