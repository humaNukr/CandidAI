package ua.edu.ukma.candidai.recruitment.service.strategy;

import org.springframework.stereotype.Component;
import ua.edu.ukma.candidai.recruitment.dto.model.InterviewDecision;
import ua.edu.ukma.candidai.recruitment.dto.response.InterviewFeedbackResponse;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;

import java.util.List;

@Component
public class EngineeringEvaluationStrategy implements CandidateEvaluationStrategy {

    private static final double MIN_ENGINEERING_SCORE = 4.0;

    @Override
    public boolean supports(JobCategory category) {
        return category == JobCategory.ENGINEERING;
    }

    @Override
    public EvaluationResult evaluate(List<InterviewFeedbackResponse> feedbacks) {
        if (feedbacks == null || feedbacks.isEmpty()) {
            return new EvaluationResult(
                    0.0,
                    InterviewDecision.REJECT,
                    "No feedbacks submitted for engineering candidate"
            );
        }

        double average = feedbacks.stream()
                .mapToInt(InterviewFeedbackResponse::technicalScore)
                .average()
                .orElse(0.0);

        boolean hasRejection = feedbacks.stream()
                .anyMatch(f -> f.decision() == InterviewDecision.REJECT);

        InterviewDecision decision = (average >= MIN_ENGINEERING_SCORE && !hasRejection)
                ? InterviewDecision.HIRE
                : InterviewDecision.REJECT;

        String reason = "Engineering evaluation completed. Average score: " + average
                + (hasRejection ? " (has REJECT feedback)" : "");

        return new EvaluationResult(average, decision, reason);
    }
}
