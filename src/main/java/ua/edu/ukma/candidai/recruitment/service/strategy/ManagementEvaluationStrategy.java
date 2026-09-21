package ua.edu.ukma.candidai.recruitment.service.strategy;

import org.springframework.stereotype.Component;
import ua.edu.ukma.candidai.recruitment.dto.model.InterviewDecision;
import ua.edu.ukma.candidai.recruitment.dto.response.InterviewFeedbackResponse;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;

import java.util.List;

@Component
public class ManagementEvaluationStrategy implements CandidateEvaluationStrategy {

    private static final double MIN_MANAGEMENT_SCORE = 3.5;

    @Override
    public boolean supports(JobCategory category) {
        return category == JobCategory.MANAGEMENT;
    }

    @Override
    public EvaluationResult evaluate(List<InterviewFeedbackResponse> feedbacks) {
        if (feedbacks == null || feedbacks.isEmpty()) {
            return new EvaluationResult(
                    0.0,
                    InterviewDecision.REJECT,
                    "No feedbacks submitted for management candidate"
            );
        }

        double average = feedbacks.stream()
                .mapToInt(InterviewFeedbackResponse::technicalScore)
                .average()
                .orElse(0.0);

        InterviewDecision decision = average >= MIN_MANAGEMENT_SCORE
                ? InterviewDecision.HIRE
                : InterviewDecision.REJECT;

        String reason = "Management evaluation completed. Average score: " + average;

        return new EvaluationResult(average, decision, reason);
    }
}
