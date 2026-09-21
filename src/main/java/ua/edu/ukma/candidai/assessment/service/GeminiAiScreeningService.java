package ua.edu.ukma.candidai.assessment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.assessment.config.GeminiProperties;
import ua.edu.ukma.candidai.assessment.dto.AiScreeningResult;
import ua.edu.ukma.candidai.assessment.dto.gemini.GeminiApiModels.GenerateContentRequest;
import ua.edu.ukma.candidai.assessment.dto.gemini.GeminiApiModels.GenerateContentResponse;
import ua.edu.ukma.candidai.assessment.dto.gemini.GeminiApiModels.GeminiScreeningPayload;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.vacancy.VacancyDetails;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiAiScreeningService implements AiScreeningService {

    private static final int JSON_PREFIX_LENGTH = 7;
    private static final int MARKDOWN_FENCE_LENGTH = 3;

    private final RestClient geminiRestClient;
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;
    private final CommonGenerator commonGenerator;

    @Override
    public AiScreeningResult screenCandidate(UUID applicationId, String resumeText, VacancyDetails vacancy) {
        String apiKey = properties.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            String error = "Gemini API key is not configured. AI screening is unavailable.";
            log.warn(error);
            return AiScreeningResult.failed(applicationId, vacancy.id(), error, commonGenerator.now());
        }

        try {
            String prompt = buildScreeningPrompt(resumeText, vacancy);
            GenerateContentRequest request = GenerateContentRequest.of(prompt);

            String url = "/v1beta/models/" + properties.model() + ":generateContent?key=" + apiKey;

            GenerateContentResponse response = geminiRestClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GenerateContentResponse.class);

            if (response == null || response.extractText() == null) {
                String error = "Empty response received from Gemini API.";
                log.warn(error);
                return AiScreeningResult.failed(applicationId, vacancy.id(), error, commonGenerator.now());
            }

            String jsonText = sanitizeJson(response.extractText());
            GeminiScreeningPayload payload = objectMapper.readValue(jsonText, GeminiScreeningPayload.class);
            payload.validate();

            return AiScreeningResult.completed(
                    applicationId,
                    vacancy.id(),
                    payload.matchingScore(),
                    payload.passed(),
                    payload.summary(),
                    payload.strengths(),
                    payload.weaknesses(),
                    payload.suggestedQuestions(),
                    commonGenerator.now()
            );
        } catch (Exception ex) {
            String error = "Gemini API call failed: " + ex.getMessage();
            log.error(error, ex);
            return AiScreeningResult.failed(applicationId, vacancy.id(), error, commonGenerator.now());
        }
    }

    private String buildScreeningPrompt(String resumeText, VacancyDetails vacancy) {
        return """
                You are a senior technical recruiter and hiring manager conducting automated resume screening.
                Evaluate how well the candidate matches the job requirements.

                Job Vacancy:
                - Title: %s
                - Seniority: %s
                - Description: %s
                - Required Skills: %s
                - Preferred Skills: %s

                Candidate Resume:
                %s

                Evaluation Instructions:
                1. Calculate matchingScore as an integer 0..100.
                2. Set passed to true if matchingScore >= 60, else false.
                3. Provide a concise summary (2-3 sentences) explaining the decision.
                4. List 2-4 candidate strengths.
                5. List 1-3 candidate weaknesses or gaps.
                6. Suggest 3-5 technical interview questions.

                Respond ONLY with a JSON object adhering to this schema:
                {
                  "matchingScore": 85,
                  "passed": true,
                  "summary": "...",
                  "strengths": ["..."],
                  "weaknesses": ["..."],
                  "suggestedQuestions": ["..."]
                }
                """.formatted(
                vacancy.title(),
                vacancy.seniorityLevel(),
                vacancy.description(),
                vacancy.requiredSkills(),
                vacancy.preferredSkills(),
                resumeText
        );
    }

    private String sanitizeJson(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(JSON_PREFIX_LENGTH);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(MARKDOWN_FENCE_LENGTH);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - MARKDOWN_FENCE_LENGTH);
        }
        return trimmed.trim();
    }
}
