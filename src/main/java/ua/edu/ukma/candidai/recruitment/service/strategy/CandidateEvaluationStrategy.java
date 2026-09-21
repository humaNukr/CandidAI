package ua.edu.ukma.candidai.recruitment.service.strategy;

import ua.edu.ukma.candidai.recruitment.dto.response.InterviewFeedbackResponse;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;

import java.util.List;

public interface CandidateEvaluationStrategy {

    boolean supports(JobCategory category);

    EvaluationResult evaluate(List<InterviewFeedbackResponse> feedbacks);
}
