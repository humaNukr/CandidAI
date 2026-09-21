package ua.edu.ukma.candidai.recruitment.service;

import lombok.RequiredArgsConstructor;
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
import java.util.List;
import java.util.UUID;

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
        if (!vacancyApi.isVacancyOpen(request.vacancyId())) {
            throw new ResourceNotFoundException("Vacancy not found or is closed with id: " + request.vacancyId());
        }

        if (applicationRepository.existsByVacancyIdAndEmail(request.vacancyId(), request.email())) {
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
                applicationId,
                request.vacancyId(),
                request.email(),
                now
        ));

        return saved;
    }

    @Override
    public ApplicationResponse getById(UUID id) {
        return findApplicationOrThrow(id);
    }

    @Override
    public ApplicationResponse updateStatus(UUID id, UpdateApplicationStatusRequest request) {
        ApplicationResponse existing = findApplicationOrThrow(id);

        ApplicationStatus currentStatus = existing.status();
        ApplicationStatus newStatus = request.status();

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new InvalidStateTransitionException(
                    "Invalid status transition from " + currentStatus + " to " + newStatus
            );
        }

        if (newStatus == ApplicationStatus.OFFER) {
            EvaluationResult evaluation = evaluateCandidate(id);
            if (evaluation.recommendedDecision() == InterviewDecision.REJECT) {
                throw new InvalidStateTransitionException(
                        "Cannot make an offer to candidate with REJECT evaluation"
                );
            }
        }

        Instant now = commonGenerator.now();
        String comment = request.comment() != null ? request.comment() : existing.comment();

        ApplicationResponse updated = new ApplicationResponse(
                existing.id(),
                existing.vacancyId(),
                existing.candidateName(),
                existing.email(),
                existing.phone(),
                existing.resumeUrl(),
                newStatus,
                comment,
                existing.appliedAt(),
                now
        );

        ApplicationResponse saved = applicationRepository.save(updated);

        eventPublisher.publishEvent(new ApplicationStatusChangedEvent(
                saved.id(),
                saved.vacancyId(),
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
        findApplicationOrThrow(id);

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

        return feedbackRepository.save(feedback);
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

        return strategy.evaluate(feedbacks);
    }

    @Override
    public List<ApplicationResponse> getApplicationsByVacancy(UUID vacancyId) {
        return applicationRepository.findByVacancyId(vacancyId);
    }

    private ApplicationResponse findApplicationOrThrow(UUID id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
    }
}
