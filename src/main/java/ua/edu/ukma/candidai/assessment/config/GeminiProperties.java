package ua.edu.ukma.candidai.assessment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "candidai.gemini")
public record GeminiProperties(
        String apiKey,
        String model,
        String baseUrl,
        int timeoutSeconds
) {
    private static final int DEFAULT_TIMEOUT_SECONDS = 15;

    public GeminiProperties {
        if (model == null || model.isBlank()) {
            model = "gemini-3.5-flash-lite";
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://generativelanguage.googleapis.com";
        }
        if (timeoutSeconds <= 0) {
            timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        }
    }
}
