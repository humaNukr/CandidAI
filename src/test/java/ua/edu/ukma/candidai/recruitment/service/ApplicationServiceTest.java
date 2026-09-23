package ua.edu.ukma.candidai.recruitment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
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
import ua.edu.ukma.candidai.recruitment.service.strategy.EngineeringEvaluationStrategy;
import ua.edu.ukma.candidai.recruitment.service.strategy.EvaluationResult;
import ua.edu.ukma.candidai.vacancy.VacancyApi;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    private static final UUID VACANCY_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID APPLICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FEEDBACK_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Instant NOW = Instant.parse("2026-09-20T10:00:00Z");

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private InterviewFeedbackRepository feedbackRepository;

    @Mock
    private VacancyApi vacancyApi;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CommonGenerator commonGenerator;

    private ApplicationServiceImpl applicationService;

    @BeforeEach
    void setUp() {
        List<CandidateEvaluationStrategy> strategies = List.of(new EngineeringEvaluationStrategy());
        applicationService = new ApplicationServiceImpl(
                applicationRepository,
                feedbackRepository,
                vacancyApi,
                eventPublisher,
                commonGenerator,
                strategies
        );
    }

    @Test
    @DisplayName("apply - should save application and publish ApplicationSubmittedEvent when valid")
    void givenValidRequest_apply_shouldSaveAndPublishEvent() {
        ApplyForVacancyRequest request = new ApplyForVacancyRequest(
                VACANCY_ID,
                "John Doe",
                "john.doe@example.com",
                "+380501234567",
                "https://storage.candidai.ukma.edu.ua/resumes/john_doe.pdf"
        );

        when(vacancyApi.isVacancyOpen(VACANCY_ID)).thenReturn(true);
        when(applicationRepository.existsByVacancyIdAndEmail(VACANCY_ID, "john.doe@example.com")).thenReturn(false);
        when(commonGenerator.uuid()).thenReturn(APPLICATION_ID);
        when(commonGenerator.now()).thenReturn(NOW);
        when(applicationRepository.save(any(ApplicationResponse.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationResponse result = applicationService.apply(request);

        assertThat(result.id()).isEqualTo(APPLICATION_ID);
        assertThat(result.status()).isEqualTo(ApplicationStatus.APPLIED);
        verify(applicationRepository).save(any(ApplicationResponse.class));

        ArgumentCaptor<ApplicationSubmittedEvent> captor
                = ArgumentCaptor.forClass(ApplicationSubmittedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ApplicationSubmittedEvent publishedEvent = captor.getValue();
        assertThat(publishedEvent.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(publishedEvent.vacancyId()).isEqualTo(VACANCY_ID);
        assertThat(publishedEvent.candidateName()).isEqualTo("John Doe");
        assertThat(publishedEvent.email()).isEqualTo("john.doe@example.com");
        assertThat(publishedEvent.resumeUrl())
                .isEqualTo("https://storage.candidai.ukma.edu.ua/resumes/john_doe.pdf");
    }

    @Test
    @DisplayName("apply - should throw DuplicateResourceException when email already applied to vacancy")
    void givenDuplicateEmail_apply_shouldThrowDuplicateResourceException() {
        ApplyForVacancyRequest request = new ApplyForVacancyRequest(
                VACANCY_ID,
                "John Doe",
                "john.doe@example.com",
                "+380501234567",
                "https://storage.candidai.ukma.edu.ua/resumes/john_doe.pdf"
        );

        when(vacancyApi.isVacancyOpen(VACANCY_ID)).thenReturn(true);
        when(applicationRepository.existsByVacancyIdAndEmail(VACANCY_ID, "john.doe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> applicationService.apply(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already applied");

        verify(applicationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("apply - should throw ResourceNotFoundException when vacancy is closed or not found")
    void givenClosedVacancy_apply_shouldThrowResourceNotFoundException() {
        ApplyForVacancyRequest request = new ApplyForVacancyRequest(
                VACANCY_ID,
                "John Doe",
                "john.doe@example.com",
                "+380501234567",
                "https://storage.candidai.ukma.edu.ua/resumes/john_doe.pdf"
        );

        when(vacancyApi.isVacancyOpen(VACANCY_ID)).thenReturn(false);

        assertThatThrownBy(() -> applicationService.apply(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Vacancy not found or is closed");

        verify(applicationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("getById - should return ApplicationResponse when found")
    void givenExistingId_getById_shouldReturnApplication() {
        ApplicationResponse app = sampleApplication(ApplicationStatus.APPLIED);

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(app));

        ApplicationResponse result = applicationService.getById(APPLICATION_ID);

        assertThat(result).isEqualTo(app);
    }

    @Test
    @DisplayName("getById - should throw ResourceNotFoundException when not found")
    void givenNonExistentId_getById_shouldThrowResourceNotFoundException() {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getById(APPLICATION_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Application not found");
    }

    @Test
    @DisplayName("updateStatus - should update status and publish ApplicationStatusChangedEvent on valid transition")
    void givenValidTransition_updateStatus_shouldUpdateAndPublishEvent() {
        ApplicationResponse existing = sampleApplication(ApplicationStatus.APPLIED);
        UpdateApplicationStatusRequest request = new UpdateApplicationStatusRequest(
                ApplicationStatus.SCREENING,
                "Passed"
        );

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existing));
        when(commonGenerator.now()).thenReturn(NOW);
        when(applicationRepository.save(any(ApplicationResponse.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationResponse result = applicationService.updateStatus(APPLICATION_ID, request);

        assertThat(result.status()).isEqualTo(ApplicationStatus.SCREENING);
        assertThat(result.comment()).isEqualTo("Passed");

        ArgumentCaptor<ApplicationStatusChangedEvent> captor
                = ArgumentCaptor.forClass(ApplicationStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ApplicationStatusChangedEvent event = captor.getValue();
        assertThat(event.previousStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(event.newStatus()).isEqualTo(ApplicationStatus.SCREENING);
        assertThat(event.comment()).isEqualTo("Passed");
    }

    @Test
    @DisplayName("updateStatus - should throw InvalidStateTransitionException on invalid transition")
    void givenInvalidTransition_updateStatus_shouldThrowException() {
        ApplicationResponse existing = sampleApplication(ApplicationStatus.APPLIED);
        UpdateApplicationStatusRequest request = new UpdateApplicationStatusRequest(
                ApplicationStatus.OFFER,
                "Jump to offer"
        );

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> applicationService.updateStatus(APPLICATION_ID, request))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Invalid status transition from APPLIED to OFFER");

        verify(applicationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("updateStatus - should throw InvalidStateTransitionException when transitioning to OFFER with REJECT")
    void givenRejectEvaluation_updateStatusToOffer_shouldThrowException() {
        ApplicationResponse existing = sampleApplication(ApplicationStatus.INTERVIEW);
        UpdateApplicationStatusRequest request = new UpdateApplicationStatusRequest(
                ApplicationStatus.OFFER,
                "Candidate did not pass but trying to offer"
        );
        InterviewFeedbackResponse rejectFeedback = new InterviewFeedbackResponse(
                FEEDBACK_ID, APPLICATION_ID, "Lead", 2, "Poor", InterviewDecision.REJECT, NOW
        );

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existing));
        when(feedbackRepository.findByApplicationId(APPLICATION_ID)).thenReturn(List.of(rejectFeedback));
        when(vacancyApi.getVacancyCategory(VACANCY_ID)).thenReturn(JobCategory.ENGINEERING);

        assertThatThrownBy(() -> applicationService.updateStatus(APPLICATION_ID, request))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot make an offer to candidate with REJECT evaluation");

        verify(applicationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("updateStatus - should succeed when transitioning to OFFER with HIRE evaluation")
    void givenHireEvaluation_updateStatusToOffer_shouldSucceed() {
        ApplicationResponse existing = sampleApplication(ApplicationStatus.INTERVIEW);
        UpdateApplicationStatusRequest request = new UpdateApplicationStatusRequest(
                ApplicationStatus.OFFER,
                "Strong candidate"
        );
        InterviewFeedbackResponse hireFeedback = new InterviewFeedbackResponse(
                FEEDBACK_ID, APPLICATION_ID, "Lead", 5, "Great", InterviewDecision.HIRE, NOW
        );

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existing));
        when(feedbackRepository.findByApplicationId(APPLICATION_ID)).thenReturn(List.of(hireFeedback));
        when(vacancyApi.getVacancyCategory(VACANCY_ID)).thenReturn(JobCategory.ENGINEERING);
        when(commonGenerator.now()).thenReturn(NOW);
        when(applicationRepository.save(any(ApplicationResponse.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationResponse result = applicationService.updateStatus(APPLICATION_ID, request);

        assertThat(result.status()).isEqualTo(ApplicationStatus.OFFER);
        verify(applicationRepository).save(any(ApplicationResponse.class));
        verify(eventPublisher).publishEvent(any(ApplicationStatusChangedEvent.class));
    }

    @Test
    @DisplayName("updateStatus - should throw ResourceNotFoundException when application not found")
    void givenNonExistentId_updateStatus_shouldThrowResourceNotFoundException() {
        UpdateApplicationStatusRequest request = new UpdateApplicationStatusRequest(
                ApplicationStatus.SCREENING, "Comment"
        );

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.updateStatus(APPLICATION_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Application not found");
    }

    @Test
    @DisplayName("getApplicationsByVacancy - should return list of applications for vacancy")
    void givenVacancyId_getApplicationsByVacancy_shouldReturnApplications() {
        ApplicationResponse app = sampleApplication(ApplicationStatus.APPLIED);
        when(applicationRepository.findByVacancyId(VACANCY_ID)).thenReturn(List.of(app));

        List<ApplicationResponse> result = applicationService.getApplicationsByVacancy(VACANCY_ID);

        assertThat(result).containsExactly(app);
        verify(applicationRepository).findByVacancyId(VACANCY_ID);
    }

    @Test
    @DisplayName("submitFeedback - should save and return feedback when application exists")
    void givenExistingApplication_submitFeedback_shouldSave() {
        ApplicationResponse existing = sampleApplication(ApplicationStatus.INTERVIEW);
        SubmitInterviewFeedbackRequest request = new SubmitInterviewFeedbackRequest(
                "Alex Lead", 4, "Strong skills", InterviewDecision.HIRE
        );

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existing));
        when(commonGenerator.uuid()).thenReturn(FEEDBACK_ID);
        when(commonGenerator.now()).thenReturn(NOW);
        when(feedbackRepository.save(any(InterviewFeedbackResponse.class))).thenAnswer(inv -> inv.getArgument(0));

        InterviewFeedbackResponse response = applicationService.submitFeedback(APPLICATION_ID, request);

        assertThat(response.id()).isEqualTo(FEEDBACK_ID);
        assertThat(response.interviewerName()).isEqualTo("Alex Lead");
        assertThat(response.technicalScore()).isEqualTo(4);
    }

    @Test
    @DisplayName("getFeedbacks - should return list of feedbacks")
    void givenExistingApplication_getFeedbacks_shouldReturnList() {
        ApplicationResponse existing = sampleApplication(ApplicationStatus.INTERVIEW);
        InterviewFeedbackResponse fb = new InterviewFeedbackResponse(
                FEEDBACK_ID, APPLICATION_ID, "Alex Lead", 5, "Great", InterviewDecision.HIRE, NOW
        );

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existing));
        when(feedbackRepository.findByApplicationId(APPLICATION_ID)).thenReturn(List.of(fb));

        List<InterviewFeedbackResponse> feedbacks = applicationService.getFeedbacks(APPLICATION_ID);

        assertThat(feedbacks).containsExactly(fb);
    }

    @Test
    @DisplayName("evaluateCandidate - should evaluate candidate using supported strategy")
    void givenCandidate_evaluateCandidate_shouldExecuteStrategy() {
        ApplicationResponse existing = sampleApplication(ApplicationStatus.INTERVIEW);
        InterviewFeedbackResponse fb1 = new InterviewFeedbackResponse(
                FEEDBACK_ID, APPLICATION_ID, "Lead 1", 5, "Great", InterviewDecision.HIRE, NOW
        );
        InterviewFeedbackResponse fb2 = new InterviewFeedbackResponse(
                UUID.randomUUID(), APPLICATION_ID, "Lead 2", 4, "Good", InterviewDecision.HIRE, NOW
        );

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existing));
        when(feedbackRepository.findByApplicationId(APPLICATION_ID)).thenReturn(List.of(fb1, fb2));
        when(vacancyApi.getVacancyCategory(VACANCY_ID)).thenReturn(JobCategory.ENGINEERING);

        EvaluationResult result = applicationService.evaluateCandidate(APPLICATION_ID);

        assertThat(result.averageScore()).isEqualTo(4.5);
        assertThat(result.recommendedDecision()).isEqualTo(InterviewDecision.HIRE);
    }

    private ApplicationResponse sampleApplication(ApplicationStatus status) {
        return new ApplicationResponse(
                APPLICATION_ID,
                VACANCY_ID,
                "John Doe",
                "john.doe@example.com",
                "+380501234567",
                "https://storage.candidai.ukma.edu.ua/resumes/john_doe.pdf",
                status,
                null,
                NOW,
                NOW
        );
    }
}
