package ua.edu.ukma.candidai.recruitment.service.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.candidai.recruitment.dto.model.InterviewDecision;
import ua.edu.ukma.candidai.recruitment.dto.response.InterviewFeedbackResponse;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CandidateEvaluationStrategyTest {

    private final EngineeringEvaluationStrategy engineeringStrategy = new EngineeringEvaluationStrategy();
    private final ManagementEvaluationStrategy managementStrategy = new ManagementEvaluationStrategy();
    private final DefaultEvaluationStrategy defaultStrategy = new DefaultEvaluationStrategy();

    @Test
    @DisplayName("supports should match expected JobCategory")
    void shouldMatchSupportedCategories() {
        assertThat(engineeringStrategy.supports(JobCategory.ENGINEERING)).isTrue();
        assertThat(engineeringStrategy.supports(JobCategory.MANAGEMENT)).isFalse();

        assertThat(managementStrategy.supports(JobCategory.MANAGEMENT)).isTrue();
        assertThat(managementStrategy.supports(JobCategory.ENGINEERING)).isFalse();

        assertThat(defaultStrategy.supports(JobCategory.DESIGN)).isTrue();
        assertThat(defaultStrategy.supports(JobCategory.ENGINEERING)).isFalse();
    }

    @Test
    @DisplayName("EngineeringStrategy should recommend HIRE when score >= 4.0 and no REJECT")
    void engineeringStrategy_givenHighScoreWithoutReject_shouldRecommendHire() {
        List<InterviewFeedbackResponse> feedbacks = List.of(
                feedback(4, InterviewDecision.HIRE),
                feedback(5, InterviewDecision.HIRE)
        );

        EvaluationResult result = engineeringStrategy.evaluate(feedbacks);

        assertThat(result.averageScore()).isEqualTo(4.5);
        assertThat(result.recommendedDecision()).isEqualTo(InterviewDecision.HIRE);
    }

    @Test
    @DisplayName("EngineeringStrategy should recommend REJECT when average score is below 4.0")
    void engineeringStrategy_givenLowScore_shouldRecommendReject() {
        List<InterviewFeedbackResponse> feedbacks = List.of(
                feedback(3, InterviewDecision.HIRE),
                feedback(4, InterviewDecision.HIRE)
        );

        EvaluationResult result = engineeringStrategy.evaluate(feedbacks);

        assertThat(result.averageScore()).isEqualTo(3.5);
        assertThat(result.recommendedDecision()).isEqualTo(InterviewDecision.REJECT);
    }

    @Test
    @DisplayName("EngineeringStrategy should recommend REJECT when any feedback has REJECT decision")
    void engineeringStrategy_givenRejectDecision_shouldRecommendReject() {
        List<InterviewFeedbackResponse> feedbacks = List.of(
                feedback(5, InterviewDecision.HIRE),
                feedback(4, InterviewDecision.REJECT)
        );

        EvaluationResult result = engineeringStrategy.evaluate(feedbacks);

        assertThat(result.recommendedDecision()).isEqualTo(InterviewDecision.REJECT);
    }

    @Test
    @DisplayName("ManagementStrategy should recommend HIRE when score >= 3.5")
    void managementStrategy_givenScoreAboveThreshold_shouldRecommendHire() {
        List<InterviewFeedbackResponse> feedbacks = List.of(
                feedback(4, InterviewDecision.HIRE),
                feedback(3, InterviewDecision.HIRE)
        );

        EvaluationResult result = managementStrategy.evaluate(feedbacks);

        assertThat(result.averageScore()).isEqualTo(3.5);
        assertThat(result.recommendedDecision()).isEqualTo(InterviewDecision.HIRE);
    }

    @Test
    @DisplayName("DefaultStrategy should recommend HIRE when score >= 3.0")
    void defaultStrategy_givenScoreAboveThreshold_shouldRecommendHire() {
        List<InterviewFeedbackResponse> feedbacks = List.of(
                feedback(3, InterviewDecision.HIRE),
                feedback(3, InterviewDecision.HIRE)
        );

        EvaluationResult result = defaultStrategy.evaluate(feedbacks);

        assertThat(result.averageScore()).isEqualTo(3.0);
        assertThat(result.recommendedDecision()).isEqualTo(InterviewDecision.HIRE);
    }

    private InterviewFeedbackResponse feedback(int score, InterviewDecision decision) {
        return new InterviewFeedbackResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Interviewer",
                score,
                "Notes",
                decision,
                Instant.now()
        );
    }
}
