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

        long hireVotes = feedbacks.stream()
                .filter(f -> f.decision() == InterviewDecision.HIRE)
                .count();
        long rejectVotes = feedbacks.size() - hireVotes;

        boolean majorityHire = hireVotes > rejectVotes;

        InterviewDecision decision = (average >= MIN_MANAGEMENT_SCORE && majorityHire)
                ? InterviewDecision.HIRE
                : InterviewDecision.REJECT;

        String reason = "Management evaluation completed. Average score: " + average
                + ", Votes: " + hireVotes + " HIRE vs " + rejectVotes + " REJECT";

        return new EvaluationResult(average, decision, reason);
    }
}
