package ua.edu.ukma.candidai.recruitment.service;

import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.request.ApplyForVacancyRequest;
import ua.edu.ukma.candidai.recruitment.dto.request.SubmitInterviewFeedbackRequest;
import ua.edu.ukma.candidai.recruitment.dto.request.UpdateApplicationStatusRequest;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;
import ua.edu.ukma.candidai.recruitment.dto.response.InterviewFeedbackResponse;
import ua.edu.ukma.candidai.recruitment.service.strategy.EvaluationResult;

import java.util.List;
import java.util.UUID;

public interface ApplicationService {

    ApplicationResponse apply(ApplyForVacancyRequest request);

    ApplicationResponse getById(UUID id);

    ApplicationResponse updateStatus(UUID id, UpdateApplicationStatusRequest request);

    ApplicationResponse updateStatus(UUID id, ApplicationStatus status, Integer matchingScore, String comment);

    default ApplicationResponse updateStatus(UUID id, ApplicationStatus status, String comment) {
        return updateStatus(id, status, null, comment);
    }

    InterviewFeedbackResponse submitFeedback(UUID id, SubmitInterviewFeedbackRequest request);

    List<InterviewFeedbackResponse> getFeedbacks(UUID id);

    EvaluationResult evaluateCandidate(UUID id);

    List<ApplicationResponse> getApplicationsByVacancy(UUID vacancyId);
}
