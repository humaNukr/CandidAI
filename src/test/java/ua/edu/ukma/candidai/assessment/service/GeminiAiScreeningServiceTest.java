package ua.edu.ukma.candidai.assessment.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import ua.edu.ukma.candidai.assessment.config.AssessmentConfig;
import ua.edu.ukma.candidai.assessment.dto.AiScreeningResult;
import ua.edu.ukma.candidai.assessment.dto.ScreeningStatus;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.vacancy.VacancyDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GeminiAiScreeningServiceTest {

    private static final UUID APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID VACANCY_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final Instant NOW = Instant.parse("2026-09-21T10:00:00Z");
    private static final int EXPECTED_SCORE = 90;

    private ChatModel chatModel;
    private CommonGenerator commonGenerator;
    private GeminiAiScreeningService service;

    @BeforeEach
    void setUp() {
        chatModel = mock(ChatModel.class);
        when(chatModel.getOptions()).thenCallRealMethod();
        commonGenerator = mock(CommonGenerator.class);
        when(commonGenerator.now()).thenReturn(NOW);

        ChatClient chatClient = new AssessmentConfig().screeningChatClient(ChatClient.builder(chatModel));
        service = new GeminiAiScreeningService(chatClient, commonGenerator);
    }

    @Test
    @DisplayName("screenCandidate - should return FAILED status when using unconfigured fallback ChatModel")
    void givenUnavailableChatModel_screenCandidate_shouldReturnFailedStatus() {
        ChatModel unavailable = new AssessmentConfig().unavailableChatModel();
        ChatClient chatClient = new AssessmentConfig().screeningChatClient(ChatClient.builder(unavailable));
        GeminiAiScreeningService unconfiguredService = new GeminiAiScreeningService(chatClient, commonGenerator);

        AiScreeningResult result = unconfiguredService.screenCandidate(
                APP_ID, "Experienced developer with Java and Spring Boot expertise", sampleVacancy()
        );

        assertThat(result.applicationId()).isEqualTo(APP_ID);
        assertThat(result.vacancyId()).isEqualTo(VACANCY_ID);
        assertThat(result.status()).isEqualTo(ScreeningStatus.FAILED);
        assertThat(result.errorMessage()).contains("Gemini API key is not configured");
    }

    @Test
    @DisplayName("screenCandidate - should map structured Spring AI response into AiScreeningResult")
    void givenValidChatModelResponse_screenCandidate_shouldReturnParsedResult() {
        String payloadJson = """
                {
                  "matchingScore": 90,
                  "passed": true,
                  "summary": "Excellent",
                  "strengths": ["Java"],
                  "weaknesses": [],
                  "suggestedQuestions": ["Q1"]
                }
                """;
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(payloadJson)))));

        AiScreeningResult result = service.screenCandidate(
                APP_ID, "Strong Java engineer with {braces}", sampleVacancy()
        );

        assertThat(result.applicationId()).isEqualTo(APP_ID);
        assertThat(result.status()).isEqualTo(ScreeningStatus.COMPLETED);
        assertThat(result.matchingScore()).isEqualTo(EXPECTED_SCORE);
        assertThat(result.passed()).isTrue();
        assertThat(result.summary()).isEqualTo("Excellent");
        assertThat(result.strengths()).containsExactly("Java");
        assertThat(result.suggestedQuestions()).containsExactly("Q1");

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        assertThat(promptCaptor.getValue().getContents()).contains("Senior Java Engineer");
        assertThat(promptCaptor.getValue().getContents()).contains("Strong Java engineer");
    }

    @Test
    @DisplayName("screenCandidate - should return FAILED status when ChatModel throws exception")
    void givenChatModelException_screenCandidate_shouldReturnFailedStatus() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("Model timeout"));

        AiScreeningResult result = service.screenCandidate(APP_ID, "Java engineer", sampleVacancy());

        assertThat(result.status()).isEqualTo(ScreeningStatus.FAILED);
        assertThat(result.errorMessage()).contains("Model timeout");
    }

    @Test
    @DisplayName("screenCandidate - should return FAILED status when ChatModel returns malformed JSON")
    void givenMalformedJson_screenCandidate_shouldReturnFailedStatus() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("not valid json {")))));

        AiScreeningResult result = service.screenCandidate(APP_ID, "Java engineer", sampleVacancy());

        assertThat(result.status()).isEqualTo(ScreeningStatus.FAILED);
        assertThat(result.errorMessage()).contains("AI screening call failed");
    }

    @Test
    @DisplayName("screenCandidate - should return FAILED status when payload fails business validation")
    void givenInvalidPayload_screenCandidate_shouldReturnFailedStatus() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("{}")))));

        AiScreeningResult result = service.screenCandidate(APP_ID, "Java engineer", sampleVacancy());

        assertThat(result.status()).isEqualTo(ScreeningStatus.FAILED);
        assertThat(result.errorMessage()).contains("Matching score must be between 0 and 100");
    }

    @Test
    @DisplayName("screenCandidate - should return FAILED status when ChatModel returns empty response")
    void givenNullResponse_screenCandidate_shouldReturnFailedStatus() {
        when(chatModel.call(any(Prompt.class))).thenReturn(null);

        AiScreeningResult result = service.screenCandidate(APP_ID, "Java engineer", sampleVacancy());

        assertThat(result.status()).isEqualTo(ScreeningStatus.FAILED);
        assertThat(result.errorMessage()).isNotBlank();
    }

    private VacancyDetails sampleVacancy() {
        return new VacancyDetails(
                VACANCY_ID,
                "Senior Java Engineer",
                "Core banking backend",
                List.of("Java", "Spring Boot"),
                List.of("Docker"),
                "Senior"
        );
    }
}
