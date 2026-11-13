package ua.edu.ukma.candidai.recruitment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.candidai.common.audit.annotation.AuditAction;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.recruitment.dto.response.InterviewFeedbackResponse;
import ua.edu.ukma.candidai.recruitment.model.Application;
import ua.edu.ukma.candidai.recruitment.model.InterviewFeedback;
import ua.edu.ukma.candidai.recruitment.repository.ApplicationRepository;
import ua.edu.ukma.candidai.recruitment.repository.InterviewFeedbackRepository;
import ua.edu.ukma.candidai.recruitment.service.strategy.CandidateEvaluationStrategy;
import ua.edu.ukma.candidai.recruitment.service.strategy.EvaluationResult;
import ua.edu.ukma.candidai.vacancy.VacancyApi;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateEvaluationServiceImpl implements CandidateEvaluationService {

    private final ApplicationRepository applicationRepository;
    private final InterviewFeedbackRepository feedbackRepository;
    private final VacancyApi vacancyApi;
    private final ApplicationMapper applicationMapper;
    private final List<CandidateEvaluationStrategy> evaluationStrategies;

    @Override
    @Transactional(readOnly = true)
    @AuditAction(action = "EVALUATE_APPLICATION", description = "AI screening evaluation")
    public EvaluationResult evaluateCandidate(UUID id) {
        log.debug("Starting candidate evaluation for application id: {}", id);
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Application not found with id: {}", id);
                    return new ResourceNotFoundException("Application not found with id: " + id);
                });

        List<InterviewFeedback> feedbacks = feedbackRepository.findByApplicationId(id);
        log.debug("Retrieved {} interview feedbacks for application id: {}", feedbacks.size(), id);
        List<InterviewFeedbackResponse> feedbackResponses = applicationMapper.toFeedbackResponseList(feedbacks);

        JobCategory category = vacancyApi.getVacancyCategory(application.getVacancyId());
        log.debug("Matching candidate evaluation strategy for category: {}", category);
        CandidateEvaluationStrategy strategy = evaluationStrategies.stream()
                .filter(s -> s.supports(category))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No strategy found for category: " + category));

        EvaluationResult result = strategy.evaluate(feedbackResponses);
        log.info("Evaluated candidate for application {} (category: {}): recommendation={}, score={}",
                id, category, result.recommendedDecision(), result.averageScore());
        return result;
    }
}
