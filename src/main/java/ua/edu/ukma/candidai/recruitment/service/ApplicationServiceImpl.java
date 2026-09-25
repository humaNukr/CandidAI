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
import ua.edu.ukma.candidai.recruitment.repository.ApplicationRepository;
import ua.edu.ukma.candidai.recruitment.repository.InterviewFeedbackRepository;
import ua.edu.ukma.candidai.recruitment.service.strategy.CandidateEvaluationStrategy;
import ua.edu.ukma.candidai.recruitment.service.strategy.EvaluationResult;
import ua.edu.ukma.candidai.vacancy.VacancyApi;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;

import java.time.Instant;
import java.util.Comparator;
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

        ApplicationResponse application = new ApplicationResponse(
                applicationId,
                request.vacancyId(),
                request.candidateName(),
                request.email(),
                request.phone(),
                request.resumeUrl(),
                ApplicationStatus.APPLIED,
                null,
                now,
                now
        );

        ApplicationResponse saved = applicationRepository.save(application);

        eventPublisher.publishEvent(new ApplicationSubmittedEvent(
                saved.id(),
                saved.vacancyId(),
                saved.candidateName(),
                saved.email(),
                saved.resumeUrl(),
                saved.appliedAt()
        ));
        log.info("Successfully created application {} for vacancy {}", saved.id(), saved.vacancyId());

        return saved;
    }

    @Override
    public ApplicationResponse getById(UUID id) {
        log.debug("Fetching application with id: {}", id);
        return findApplicationOrThrow(id);
    }

    @Override
    public ApplicationResponse updateStatus(UUID id, UpdateApplicationStatusRequest request) {
        ApplicationResponse existing = findApplicationOrThrow(id);

        ApplicationStatus currentStatus = existing.status();
        ApplicationStatus newStatus = request.status();

        if (!currentStatus.canTransitionTo(newStatus)) {
            log.warn("Invalid status transition attempt from {} to {} for application {}",
                    currentStatus, newStatus, id);
            throw new InvalidStateTransitionException(
                    "Invalid status transition from " + currentStatus + " to " + newStatus
            );
        }

        if (newStatus == ApplicationStatus.OFFER) {
            EvaluationResult evaluation = evaluateCandidate(id);
            if (evaluation.recommendedDecision() == InterviewDecision.REJECT) {
                log.warn("Blocked transition to OFFER for application {}: evaluation result was REJECT", id);
                throw new InvalidStateTransitionException(
                        "Cannot make an offer to candidate with REJECT evaluation"
                );
            }
        }

        Instant now = commonGenerator.now();
        String comment = request.comment() != null ? request.comment() : existing.comment();
        Integer score = request.matchingScore() != null ? request.matchingScore() : existing.matchingScore();

        ApplicationResponse updated = new ApplicationResponse(
                existing.id(),
                existing.vacancyId(),
                existing.candidateName(),
                existing.email(),
                existing.phone(),
                existing.resumeUrl(),
                newStatus,
                score,
                comment,
                existing.appliedAt(),
                now
        );

        ApplicationResponse saved = applicationRepository.save(updated);

        log.info("Updated status for application {} from {} to {}", saved.id(), currentStatus, newStatus);

        eventPublisher.publishEvent(new ApplicationStatusChangedEvent(
                saved.id(),
                saved.vacancyId(),
                saved.candidateName(),
                saved.email(),
                currentStatus,
                newStatus,
                comment,
                now
        ));

        return saved;
    }

    @Override
    public InterviewFeedbackResponse submitFeedback(UUID id, SubmitInterviewFeedbackRequest request) {
        ApplicationResponse application = findApplicationOrThrow(id);

        if (application.status() != ApplicationStatus.INTERVIEW) {
            throw new InvalidStateTransitionException(
                    "Cannot submit feedback for application in status: " + application.status()
                            + ". Expected: INTERVIEW"
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
        ApplicationResponse application = findApplicationOrThrow(id);
        List<InterviewFeedbackResponse> feedbacks = feedbackRepository.findByApplicationId(id);

        JobCategory category = vacancyApi.getVacancyCategory(application.vacancyId());
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
        return getApplicationsByVacancy(vacancyId, false);
    }

    @Override
    public List<ApplicationResponse> getApplicationsByVacancy(UUID vacancyId, boolean sortByScore) {
        List<ApplicationResponse> applications = applicationRepository.findByVacancyId(vacancyId);
        if (sortByScore) {
            return applications.stream()
                    .sorted(Comparator.comparing(
                            ApplicationResponse::matchingScore,
                            Comparator.nullsLast(Comparator.reverseOrder())
                    ))
                    .toList();
        }
        return applications;
    }

    private ApplicationResponse findApplicationOrThrow(UUID id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Application not found with id: {}", id);
                    return new ResourceNotFoundException("Application not found with id: " + id);
                });
    }
}
