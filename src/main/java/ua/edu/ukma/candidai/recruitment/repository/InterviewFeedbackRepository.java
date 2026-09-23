package ua.edu.ukma.candidai.recruitment.repository;

import ua.edu.ukma.candidai.recruitment.dto.response.InterviewFeedbackResponse;

import java.util.List;
import java.util.UUID;

public interface InterviewFeedbackRepository {

    InterviewFeedbackResponse save(InterviewFeedbackResponse feedback);

    List<InterviewFeedbackResponse> findByApplicationId(UUID applicationId);
}
