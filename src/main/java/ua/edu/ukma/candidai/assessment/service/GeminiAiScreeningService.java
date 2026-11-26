package ua.edu.ukma.candidai.assessment.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import ua.edu.ukma.candidai.assessment.dto.AiScreeningResult;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.vacancy.VacancyDetails;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiAiScreeningService implements AiScreeningService {

    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 100;
    private static final String USER_PROMPT_TEMPLATE = """
            Evaluate how well the candidate matches the job requirements.

            Job Vacancy:
            - Title: {title}
            - Seniority: {seniority}
            - Description: {description}
            - Required Skills: {requiredSkills}
            - Preferred Skills: {preferredSkills}

            Candidate Resume:
            {resumeText}

            Evaluation Instructions:
            1. Calculate matchingScore as an integer from 0 to 100.
            2. Set passed to true if matchingScore >= 60, else false.
            3. Provide a concise summary (2-3 sentences) explaining the decision.
            4. List 2-4 candidate strengths.
            5. List 1-3 candidate weaknesses or gaps.
            6. Suggest 3-5 technical interview questions.
            """;

    private final ChatClient screeningChatClient;
    private final CommonGenerator commonGenerator;

    @Override
    public AiScreeningResult screenCandidate(UUID applicationId, String resumeText, VacancyDetails vacancy) {
        try {
            log.info("Sending screening request via Spring AI ChatClient for application: {}", applicationId);

            ScreeningEvaluationPayload payload = screeningChatClient.prompt()
                    .user(userSpec -> userSpec
                            .text(USER_PROMPT_TEMPLATE)
                            .param("title", nullToEmpty(vacancy.title()))
                            .param("seniority", nullToEmpty(vacancy.seniorityLevel()))
                            .param("description", nullToEmpty(vacancy.description()))
                            .param("requiredSkills", String.valueOf(vacancy.requiredSkills()))
                            .param("preferredSkills", String.valueOf(vacancy.preferredSkills()))
                            .param("resumeText", nullToEmpty(resumeText)))
                    .call()
                    .entity(ScreeningEvaluationPayload.class);

            if (payload == null) {
                String error = "Empty response received from AI model.";
                log.warn(error);
                return AiScreeningResult.failed(applicationId, vacancy.id(), error, commonGenerator.now());
            }

            payload.validate();

            log.info("Received screening response for application {}: score={}/100, passed={}",
                    applicationId, payload.matchingScore(), payload.passed());

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
            String error = "AI screening call failed: " + ex.getMessage();
            log.error(error, ex);
            return AiScreeningResult.failed(applicationId, vacancy.id(), error, commonGenerator.now());
        }
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    public record ScreeningEvaluationPayload(
            Integer matchingScore,
            Boolean passed,
            String summary,
            List<String> strengths,
            List<String> weaknesses,
            List<String> suggestedQuestions
    ) {
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
