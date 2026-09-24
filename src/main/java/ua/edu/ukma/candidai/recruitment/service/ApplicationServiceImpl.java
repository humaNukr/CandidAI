package ua.edu.ukma.candidai.recruitment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ua.edu.ukma.candidai.common.exception.DuplicateResourceException;
import ua.edu.ukma.candidai.common.exception.InvalidStateTransitionException;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.model.InterviewDecision;
import ua.edu.ukma.candidai.recruitment.dto.request.ApplyForVacancyRequest;
import ua.edu.ukma.candidai.recruitment.dto.request.SubmitInterviewFeedbackRequest;
import ua.edu.ukma.candidai.recruitment.dto.request.UpdateApplicationStatusRequest;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;
import ua.edu.ukma.candidai.recruitment.dto.response.InterviewFeedbackResponse;
import ua.edu.ukma.candidai.recruitment.event.ApplicationStatusChangedEvent;
import ua.edu.ukma.candidai.recruitment.event.ApplicationSubmittedEvent;
import ua.edu.ukma.candidai.recruitment.model.Application;
import ua.edu.ukma.candidai.recruitment.repository.ApplicationRepository;
import ua.edu.ukma.candidai.recruitment.repository.InterviewFeedbackRepository;
import ua.edu.ukma.candidai.recruitment.service.strategy.CandidateEvaluationStrategy;
import ua.edu.ukma.candidai.recruitment.service.strategy.EvaluationResult;
import ua.edu.ukma.candidai.vacancy.VacancyApi;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final InterviewFeedbackRepository feedbackRepository;
    private final VacancyApi vacancyApi;
    private final ApplicationEventPublisher eventPublisher;
    private final CommonGenerator commonGenerator;
    private final ApplicationMapper applicationMapper;
    private final List<CandidateEvaluationStrategy> evaluationStrategies;

    @Override
    public ApplicationResponse apply(ApplyForVacancyRequest request) {
        log.info("Processing job application for candidate '{}' on vacancy: {}",
                request.candidateName(), request.vacancyId());

        if (!vacancyApi.isVacancyOpen(request.vacancyId())) {
            log.warn("Application rejected: vacancy {} is not open or not found", request.vacancyId());
            throw new ResourceNotFoundException("Vacancy not found or is closed with id: " + request.vacancyId());
        }

        if (applicationRepository.existsByVacancyIdAndEmail(request.vacancyId(), request.email())) {
            log.warn("Application rejected: candidate with email {} already applied to vacancy {}",
                    request.email(), request.vacancyId());
            throw new DuplicateResourceException(
                    "Candidate with email " + request.email() + " has already applied to vacancy " + request.vacancyId()
            );
        }
        UUID applicationId = commonGenerator.uuid();
        Instant now = commonGenerator.now();

        Application application = Application.create(request, applicationId, now);
        Application saved = applicationRepository.save(application);

        eventPublisher.publishEvent(new ApplicationSubmittedEvent(
                saved.getId(),
                saved.getVacancyId(),
                saved.getCandidateName(),
                saved.getEmail(),
                saved.getResumeUrl(),
                saved.getAppliedAt()
        ));
        log.info("Successfully created application {} for vacancy {}", saved.getId(), saved.getVacancyId());

        return applicationMapper.toResponse(saved);
    }

    @Override
    public ApplicationResponse getById(UUID id) {
        log.debug("Fetching application with id: {}", id);
        return applicationMapper.toResponse(findApplicationOrThrow(id));
    }

    @Override
    public ApplicationResponse updateStatus(UUID id, UpdateApplicationStatusRequest request) {
        Application existing = findApplicationOrThrow(id);
        ApplicationStatus currentStatus = existing.getStatus();
        ApplicationStatus newStatus = request.status();

        if (newStatus == ApplicationStatus.OFFER) {
            if (!currentStatus.canTransitionTo(newStatus)) {
                log.warn("Invalid status transition attempt from {} to {} for application {}",
                        currentStatus, newStatus, id);
                throw new InvalidStateTransitionException(
                        "Invalid status transition from " + currentStatus + " to " + newStatus
                );
            }
            EvaluationResult evaluation = evaluateCandidate(id);
            if (evaluation.recommendedDecision() == InterviewDecision.REJECT) {
                log.warn("Blocked transition to OFFER for application {}: evaluation result was REJECT", id);
                throw new InvalidStateTransitionException(
                        "Cannot make an offer to candidate with REJECT evaluation"
                );
            }
        }

        Instant now = commonGenerator.now();
        existing.updateStatus(newStatus, request.matchingScore(), request.comment(), now);

        Application saved = applicationRepository.save(existing);
        log.info("Updated status for application {} from {} to {}", saved.getId(), currentStatus, newStatus);

        eventPublisher.publishEvent(new ApplicationStatusChangedEvent(
                saved.getId(),
                saved.getVacancyId(),
                saved.getEmail(),
                currentStatus,
                newStatus,
                saved.getComment(),
                now
        ));

        return applicationMapper.toResponse(saved);
    }

    @Override
    public ApplicationResponse updateStatus(UUID id, ApplicationStatus status, Integer matchingScore, String comment) {
        return updateStatus(id, new UpdateApplicationStatusRequest(status, comment, matchingScore));
    }

    @Override
    public InterviewFeedbackResponse submitFeedback(UUID id, SubmitInterviewFeedbackRequest request) {
        Application application = findApplicationOrThrow(id);

        if (application.getStatus() != ApplicationStatus.INTERVIEW) {
            log.warn("Cannot submit interview feedback for application {} in status {}", id, application.getStatus());
            throw new InvalidStateTransitionException(
                    "Cannot submit interview feedback for application " + id + " in status " + application.getStatus()
            );
        }

        UUID feedbackId = commonGenerator.uuid();
        Instant now = commonGenerator.now();

        InterviewFeedbackResponse feedback = new InterviewFeedbackResponse(
                feedbackId,
                id,
                request.interviewerName(),
                request.technicalScore(),
                request.notes(),
                request.decision(),
                now
        );

        InterviewFeedbackResponse saved = feedbackRepository.save(feedback);
        log.info("Saved interview feedback {} for application {}: decision={}, score={}",
                saved.id(), id, saved.decision(), saved.technicalScore());
        return saved;
    }

    @Override
    public List<InterviewFeedbackResponse> getFeedbacks(UUID id) {
        findApplicationOrThrow(id);
        return feedbackRepository.findByApplicationId(id);
    }

    @Override
    public EvaluationResult evaluateCandidate(UUID id) {
        Application application = findApplicationOrThrow(id);
        List<InterviewFeedbackResponse> feedbacks = feedbackRepository.findByApplicationId(id);

        JobCategory category = vacancyApi.getVacancyCategory(application.getVacancyId());
        if (category == null) {
            category = JobCategory.ENGINEERING;
        }

        final JobCategory targetCategory = category;
        CandidateEvaluationStrategy strategy = evaluationStrategies.stream()
                .filter(s -> s.supports(targetCategory))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No strategy found for category: " + targetCategory));

        EvaluationResult result = strategy.evaluate(feedbacks);
        log.info("Evaluated candidate for application {} (category: {}): recommendation={}, score={}",
                id, targetCategory, result.recommendedDecision(), result.averageScore());
        return result;
    }

    @Override
    public List<ApplicationResponse> getApplicationsByVacancy(UUID vacancyId) {
        return applicationRepository.findByVacancyId(vacancyId).stream()
                .map(applicationMapper::toResponse)
                .toList();
    }

    private Application findApplicationOrThrow(UUID id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Application not found with id: {}", id);
                    return new ResourceNotFoundException("Application not found with id: " + id);
                });
    }
}
