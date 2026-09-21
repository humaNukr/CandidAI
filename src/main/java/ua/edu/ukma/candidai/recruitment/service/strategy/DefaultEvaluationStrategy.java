package ua.edu.ukma.candidai.recruitment.service.strategy;

import org.springframework.stereotype.Component;
import ua.edu.ukma.candidai.recruitment.dto.model.InterviewDecision;
import ua.edu.ukma.candidai.recruitment.dto.response.InterviewFeedbackResponse;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;

import java.util.List;

@Component
public class DefaultEvaluationStrategy implements CandidateEvaluationStrategy {

    private static final double MIN_DEFAULT_SCORE = 3.0;

    @Override
    public boolean supports(JobCategory category) {
        return category != JobCategory.ENGINEERING && category != JobCategory.MANAGEMENT;
    }

    @Override
    public EvaluationResult evaluate(List<InterviewFeedbackResponse> feedbacks) {
        if (feedbacks == null || feedbacks.isEmpty()) {
            return new EvaluationResult(0.0, InterviewDecision.REJECT, "No feedbacks submitted for candidate");
        }

        double average = feedbacks.stream()
                .mapToInt(InterviewFeedbackResponse::technicalScore)
                .average()
                .orElse(0.0);

        InterviewDecision decision = average >= MIN_DEFAULT_SCORE
                ? InterviewDecision.HIRE
                : InterviewDecision.REJECT;

        String reason = "Default evaluation completed. Average score: " + average;

        return new EvaluationResult(average, decision, reason);
    }
}
