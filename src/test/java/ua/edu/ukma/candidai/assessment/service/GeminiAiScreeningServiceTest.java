package ua.edu.ukma.candidai.assessment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.assessment.config.GeminiProperties;
import ua.edu.ukma.candidai.assessment.dto.AiScreeningResult;
import ua.edu.ukma.candidai.assessment.dto.ScreeningStatus;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.vacancy.VacancyDetails;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiAiScreeningServiceTest {

    private static final UUID APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID VACANCY_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final Instant NOW = Instant.parse("2026-09-21T10:00:00Z");

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper;
    private CommonGenerator commonGenerator;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        objectMapper = new ObjectMapper();
        commonGenerator = mock(CommonGenerator.class);
        when(commonGenerator.now()).thenReturn(NOW);
    }

    @Test
    @DisplayName("screenCandidate - should return FAILED status when API key is null or blank")
    void givenNoApiKey_screenCandidate_shouldReturnFailedStatus() {
        GeminiProperties properties = new GeminiProperties(
                null, "gemini-3.5-flash-lite", "https://generativelanguage.googleapis.com", 15
        );
        GeminiAiScreeningService service = new GeminiAiScreeningService(
                restClientBuilder.build(), properties, objectMapper, commonGenerator
        );

        VacancyDetails vacancy = new VacancyDetails(
                VACANCY_ID, "Senior Java Engineer", "Core banking backend",
                List.of("Java", "Spring Boot", "PostgreSQL"), List.of("Docker"), "Senior"
        );

        AiScreeningResult result = service.screenCandidate(
                APP_ID, "Experienced developer with Java and Spring Boot expertise", vacancy
        );

        assertThat(result.applicationId()).isEqualTo(APP_ID);
        assertThat(result.vacancyId()).isEqualTo(VACANCY_ID);
        assertThat(result.status()).isEqualTo(ScreeningStatus.FAILED);
        assertThat(result.errorMessage()).contains("Gemini API key is not configured");
    }

    @Test
    @DisplayName("screenCandidate - should parse structured Gemini JSON response successfully")
    void givenApiKeyAndValidGeminiResponse_screenCandidate_shouldReturnParsedResult() {
        GeminiProperties properties = new GeminiProperties(
                "test-api-key", "gemini-3.5-flash-lite", "https://generativelanguage.googleapis.com", 15
        );
        GeminiAiScreeningService service = new GeminiAiScreeningService(
                restClientBuilder.build(), properties, objectMapper, commonGenerator
        );

        VacancyDetails vacancy = new VacancyDetails(
                VACANCY_ID, "Senior Java Engineer", "Core banking backend",
                List.of("Java", "Spring Boot"), List.of("Docker"), "Senior"
        );

        String payloadJson = "{\"matchingScore\": 90, \"passed\": true, \"summary\": \"Excellent\", "
                + "\"strengths\": [\"Java\"], \"weaknesses\": [], \"suggestedQuestions\": [\"Q1\"]}";
        String geminiResponseBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": %s
                          }
                        ]
                      }
                    }
                  ]
                }
                """.formatted(objectMapper.writeValueAsString(payloadJson));

        String expectedUri = "https://generativelanguage.googleapis.com"
                + "/v1beta/models/gemini-3.5-flash-lite:generateContent";

        mockServer.expect(requestTo(expectedUri))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-api-key"))
                .andRespond(withSuccess(geminiResponseBody, MediaType.APPLICATION_JSON));

        AiScreeningResult result = service.screenCandidate(
                APP_ID, "Strong Java engineer", vacancy
        );

        assertThat(result.applicationId()).isEqualTo(APP_ID);
        assertThat(result.status()).isEqualTo(ScreeningStatus.COMPLETED);
        assertThat(result.matchingScore()).isEqualTo(90);
        assertThat(result.passed()).isTrue();
        assertThat(result.summary()).isEqualTo("Excellent");
        assertThat(result.strengths()).containsExactly("Java");
        assertThat(result.suggestedQuestions()).containsExactly("Q1");

        mockServer.verify();
    }

    @Test
    @DisplayName("screenCandidate - should return FAILED status when Gemini server returns 500 error")
    void givenApiServerError_screenCandidate_shouldReturnFailedStatus() {
        GeminiProperties properties = new GeminiProperties(
                "test-api-key", "gemini-3.5-flash-lite", "https://generativelanguage.googleapis.com", 15
        );
        GeminiAiScreeningService service = new GeminiAiScreeningService(
                restClientBuilder.build(), properties, objectMapper, commonGenerator
        );

        VacancyDetails vacancy = new VacancyDetails(
                VACANCY_ID, "Senior Java Engineer", "Core banking backend",
                List.of("Java"), List.of(), "Senior"
        );

        String expectedUri = "https://generativelanguage.googleapis.com"
                + "/v1beta/models/gemini-3.5-flash-lite:generateContent";

        mockServer.expect(requestTo(expectedUri))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-api-key"))
                .andRespond(withServerError());

        AiScreeningResult result = service.screenCandidate(APP_ID, "Java engineer", vacancy);

        assertThat(result.status()).isEqualTo(ScreeningStatus.FAILED);
        assertThat(result.errorMessage()).contains("Gemini API call failed");
        mockServer.verify();
    }

    @Test
    @DisplayName("screenCandidate - should return FAILED status when response is malformed JSON")
    void givenMalformedJson_screenCandidate_shouldReturnFailedStatus() {
        GeminiProperties properties = new GeminiProperties(
                "test-api-key", "gemini-3.5-flash-lite", "https://generativelanguage.googleapis.com", 15
        );
        GeminiAiScreeningService service = new GeminiAiScreeningService(
                restClientBuilder.build(), properties, objectMapper, commonGenerator
        );

        VacancyDetails vacancy = new VacancyDetails(
                VACANCY_ID, "Senior Java Engineer", "Core banking backend",
                List.of("Java"), List.of(), "Senior"
        );

        String geminiResponseBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "this is not a valid json at all {"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        String expectedUri = "https://generativelanguage.googleapis.com"
                + "/v1beta/models/gemini-3.5-flash-lite:generateContent";

        mockServer.expect(requestTo(expectedUri))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-api-key"))
                .andRespond(withSuccess(geminiResponseBody, MediaType.APPLICATION_JSON));

        AiScreeningResult result = service.screenCandidate(APP_ID, "Java engineer", vacancy);

        assertThat(result.status()).isEqualTo(ScreeningStatus.FAILED);
        assertThat(result.errorMessage()).contains("Gemini API call failed");
        mockServer.verify();
    }

    @Test
    @DisplayName("screenCandidate - should return FAILED status when payload fails business validation")
    void givenInvalidPayload_screenCandidate_shouldReturnFailedStatus() {
        GeminiProperties properties = new GeminiProperties(
                "test-api-key", "gemini-3.5-flash-lite", "https://generativelanguage.googleapis.com", 15
        );
        GeminiAiScreeningService service = new GeminiAiScreeningService(
                restClientBuilder.build(), properties, objectMapper, commonGenerator
        );

        VacancyDetails vacancy = new VacancyDetails(
                VACANCY_ID, "Senior Java Engineer", "Core banking backend",
                List.of("Java"), List.of(), "Senior"
        );

        String emptyPayloadJson = "{}";
        String geminiResponseBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": %s
                          }
                        ]
                      }
                    }
                  ]
                }
                """.formatted(objectMapper.writeValueAsString(emptyPayloadJson));

        String expectedUri = "https://generativelanguage.googleapis.com"
                + "/v1beta/models/gemini-3.5-flash-lite:generateContent";

        mockServer.expect(requestTo(expectedUri))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-api-key"))
                .andRespond(withSuccess(geminiResponseBody, MediaType.APPLICATION_JSON));

        AiScreeningResult result = service.screenCandidate(APP_ID, "Java engineer", vacancy);

        assertThat(result.status()).isEqualTo(ScreeningStatus.FAILED);
        assertThat(result.errorMessage()).contains("Matching score must be between 0 and 100");
        mockServer.verify();
    }
}

