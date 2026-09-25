package ua.edu.ukma.candidai.recruitment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import ua.edu.ukma.candidai.common.exception.DuplicateResourceException;
import ua.edu.ukma.candidai.common.exception.InvalidStateTransitionException;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.recruitment.ApplicationStatusChangedEvent;
import ua.edu.ukma.candidai.recruitment.ApplicationSubmittedEvent;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.model.InterviewDecision;
import ua.edu.ukma.candidai.recruitment.dto.request.ApplyForVacancyRequest;
import ua.edu.ukma.candidai.recruitment.dto.request.SubmitInterviewFeedbackRequest;
import ua.edu.ukma.candidai.recruitment.dto.request.UpdateApplicationStatusRequest;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;
import ua.edu.ukma.candidai.recruitment.dto.response.InterviewFeedbackResponse;
import ua.edu.ukma.candidai.recruitment.model.Application;
import ua.edu.ukma.candidai.recruitment.repository.ApplicationRepository;
import ua.edu.ukma.candidai.recruitment.repository.InterviewFeedbackRepository;
import ua.edu.ukma.candidai.recruitment.service.strategy.CandidateEvaluationStrategy;
import ua.edu.ukma.candidai.recruitment.service.strategy.EvaluationResult;
import ua.edu.ukma.candidai.vacancy.VacancyApi;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ua.edu.ukma.candidai.recruitment.RecruitmentTestResources.DEFAULT_APPLICATION_ID;
import static ua.edu.ukma.candidai.recruitment.RecruitmentTestResources.DEFAULT_CANDIDATE_ID;
import static ua.edu.ukma.candidai.recruitment.RecruitmentTestResources.DEFAULT_FEEDBACK_ID;
import static ua.edu.ukma.candidai.recruitment.RecruitmentTestResources.DEFAULT_NOW;
import static ua.edu.ukma.candidai.recruitment.RecruitmentTestResources.DEFAULT_VACANCY_ID;
import static ua.edu.ukma.candidai.recruitment.RecruitmentTestResources.NON_EXISTENT_ID;
import static ua.edu.ukma.candidai.recruitment.RecruitmentTestResources.aFeedbackResponse;
import static ua.edu.ukma.candidai.recruitment.RecruitmentTestResources.aHireFeedbackResponse;
import static ua.edu.ukma.candidai.recruitment.RecruitmentTestResources.aRejectFeedbackResponse;
import static ua.edu.ukma.candidai.recruitment.RecruitmentTestResources.anApplication;
import static ua.edu.ukma.candidai.recruitment.RecruitmentTestResources.anApplicationBuilder;
import static ua.edu.ukma.candidai.recruitment.RecruitmentTestResources.anApplicationResponse;
import static ua.edu.ukma.candidai.recruitment.RecruitmentTestResources.anEvaluationResult;
import static ua.edu.ukma.candidai.recruitment.RecruitmentTestResources.validApplyForVacancyRequest;
import static ua.edu.ukma.candidai.recruitment.RecruitmentTestResources.validSubmitFeedbackRequest;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

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

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private CandidateEvaluationStrategy evaluationStrategy;

    private ApplicationServiceImpl applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new ApplicationServiceImpl(
                applicationRepository,
                feedbackRepository,
                vacancyApi,
                eventPublisher,
                commonGenerator,
                applicationMapper,
                List.of(evaluationStrategy)
        );
    }

    @Test
    @DisplayName("apply with valid request should save application, publish event, and return response")
    void givenValidRequest_apply_shouldSaveAndPublishEvent() {
        ApplyForVacancyRequest request = validApplyForVacancyRequest();
        Application application = anApplication();
        ApplicationResponse expectedResponse = anApplicationResponse();

        when(vacancyApi.isVacancyOpen(DEFAULT_VACANCY_ID)).thenReturn(true);
        when(applicationRepository.existsByVacancyIdAndEmail(DEFAULT_VACANCY_ID, request.email())).thenReturn(false);
        when(commonGenerator.uuid()).thenReturn(DEFAULT_APPLICATION_ID);
        when(commonGenerator.now()).thenReturn(DEFAULT_NOW);
        when(applicationRepository.save(application)).thenReturn(application);
        when(applicationMapper.toResponse(application)).thenReturn(expectedResponse);

        ApplicationResponse actual = applicationService.apply(request);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expectedResponse);
        verify(eventPublisher).publishEvent(new ApplicationSubmittedEvent(
                DEFAULT_APPLICATION_ID,
                DEFAULT_VACANCY_ID,
                DEFAULT_CANDIDATE_ID,
                request.candidateName(),
                request.email(),
                request.resumeUrl(),
                DEFAULT_NOW
        ));
    }

    @Test
    @DisplayName("apply with duplicate email for vacancy should throw DuplicateResourceException")
    void givenDuplicateEmail_apply_shouldThrowDuplicateResourceException() {
        ApplyForVacancyRequest request = validApplyForVacancyRequest();

        when(vacancyApi.isVacancyOpen(DEFAULT_VACANCY_ID)).thenReturn(true);
        when(applicationRepository.existsByVacancyIdAndEmail(DEFAULT_VACANCY_ID, request.email())).thenReturn(true);

        assertThatThrownBy(() -> applicationService.apply(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already applied");
    }

    @Test
    @DisplayName("apply with closed vacancy should throw ResourceNotFoundException")
    void givenClosedVacancy_apply_shouldThrowResourceNotFoundException() {
        ApplyForVacancyRequest request = validApplyForVacancyRequest();

        when(vacancyApi.isVacancyOpen(DEFAULT_VACANCY_ID)).thenReturn(false);

        assertThatThrownBy(() -> applicationService.apply(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Vacancy not found or is closed");
    }

    @Test
    @DisplayName("getById with existing id should return response")
    void givenExistingId_getById_shouldReturnResponse() {
        Application application = anApplication();
        ApplicationResponse expectedResponse = anApplicationResponse();

        when(applicationRepository.findById(DEFAULT_APPLICATION_ID)).thenReturn(Optional.of(application));
        when(applicationMapper.toResponse(application)).thenReturn(expectedResponse);

        ApplicationResponse actual = applicationService.getById(DEFAULT_APPLICATION_ID);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("getById with non-existent id should throw ResourceNotFoundException")
    void givenNonExistentId_getById_shouldThrowResourceNotFoundException() {
        when(applicationRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getById(NON_EXISTENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Application not found with id: " + NON_EXISTENT_ID);
    }

    @Test
    @DisplayName("updateStatus with valid transition should update status, publish event, and return response")
    void givenValidTransition_updateStatus_shouldUpdateAndReturnResponse() {
        Application existing = anApplication(ApplicationStatus.APPLIED);
        UpdateApplicationStatusRequest request = new UpdateApplicationStatusRequest(
                ApplicationStatus.SCREENING,
                "Passed screening"
        );
        Instant updatedAt = DEFAULT_NOW.plusSeconds(3600);
        Application updatedApplication = anApplication(
                ApplicationStatus.SCREENING,
                null,
                "Passed screening",
                updatedAt
        );
        ApplicationResponse expectedResponse = anApplicationResponse(
                ApplicationStatus.SCREENING,
                null,
                "Passed screening",
                updatedAt
        );

        when(applicationRepository.findById(DEFAULT_APPLICATION_ID)).thenReturn(Optional.of(existing));
        when(commonGenerator.now()).thenReturn(updatedAt);
        when(applicationRepository.save(existing)).thenReturn(updatedApplication);
        when(applicationMapper.toResponse(updatedApplication)).thenReturn(expectedResponse);

        ApplicationResponse actual = applicationService.updateStatus(DEFAULT_APPLICATION_ID, request);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expectedResponse);
        verify(eventPublisher).publishEvent(new ApplicationStatusChangedEvent(
                DEFAULT_APPLICATION_ID,
                DEFAULT_VACANCY_ID,
                DEFAULT_CANDIDATE_ID,
                existing.getCandidateName(),
                existing.getEmail(),
                ApplicationStatus.APPLIED,
                ApplicationStatus.SCREENING,
                "Passed screening",
                updatedAt
        ));
    }

    @Test
    @DisplayName("updateStatus with matchingScore should update score and return response")
    void givenMatchingScore_updateStatus_shouldUpdateMatchingScoreAndReturnResponse() {
        Application existing = anApplication(ApplicationStatus.APPLIED);
        UpdateApplicationStatusRequest request = new UpdateApplicationStatusRequest(
                ApplicationStatus.SCREENING,
                "Passed screening with high score",
                85
        );
        Instant updatedAt = DEFAULT_NOW.plusSeconds(3600);
        Application updatedApplication = anApplication(
                ApplicationStatus.SCREENING,
                85,
                "Passed screening with high score",
                updatedAt
        );
        ApplicationResponse expectedResponse = anApplicationResponse(
                ApplicationStatus.SCREENING,
                85,
                "Passed screening with high score",
                updatedAt
        );

        when(applicationRepository.findById(DEFAULT_APPLICATION_ID)).thenReturn(Optional.of(existing));
        when(commonGenerator.now()).thenReturn(updatedAt);
        when(applicationRepository.save(existing)).thenReturn(updatedApplication);
        when(applicationMapper.toResponse(updatedApplication)).thenReturn(expectedResponse);

        ApplicationResponse actual = applicationService.updateStatus(DEFAULT_APPLICATION_ID, request);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("updateStatus overloaded method should update status with score and return response")
    void givenDirectStatusAndMatchingScore_updateStatus_shouldUpdateApplication() {
        Application existing = anApplication(ApplicationStatus.APPLIED);
        Instant updatedAt = DEFAULT_NOW.plusSeconds(3600);
        Application updatedApplication = anApplication(
                ApplicationStatus.SCREENING,
                92,
                "Direct screening update",
                updatedAt
        );
        ApplicationResponse expectedResponse = anApplicationResponse(
                ApplicationStatus.SCREENING,
                92,
                "Direct screening update",
                updatedAt
        );

        when(applicationRepository.findById(DEFAULT_APPLICATION_ID)).thenReturn(Optional.of(existing));
        when(commonGenerator.now()).thenReturn(updatedAt);
        when(applicationRepository.save(existing)).thenReturn(updatedApplication);
        when(applicationMapper.toResponse(updatedApplication)).thenReturn(expectedResponse);

        ApplicationResponse actual = applicationService.updateStatus(
                DEFAULT_APPLICATION_ID,
                ApplicationStatus.SCREENING,
                92,
                "Direct screening update"
        );

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("updateStatus with invalid transition should throw InvalidStateTransitionException")
    void givenInvalidTransition_updateStatus_shouldThrowInvalidStateTransitionException() {
        Application existing = anApplication(ApplicationStatus.APPLIED);
        UpdateApplicationStatusRequest request = new UpdateApplicationStatusRequest(
                ApplicationStatus.OFFER,
                "Jump to offer"
        );

        when(applicationRepository.findById(DEFAULT_APPLICATION_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> applicationService.updateStatus(DEFAULT_APPLICATION_ID, request))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Invalid status transition from APPLIED to OFFER");
    }

    @Test
    @DisplayName("updateStatus to OFFER with REJECT evaluation should throw InvalidStateTransitionException")
    void givenRejectEvaluation_updateStatus_shouldThrowInvalidStateTransitionException() {
        Application existing = anApplication(ApplicationStatus.INTERVIEW);
        UpdateApplicationStatusRequest request = new UpdateApplicationStatusRequest(
                ApplicationStatus.OFFER,
                "Candidate did not pass but trying to offer"
        );
        InterviewFeedbackResponse rejectFeedback = aRejectFeedbackResponse();

        when(applicationRepository.findById(DEFAULT_APPLICATION_ID)).thenReturn(Optional.of(existing));
        when(feedbackRepository.findByApplicationId(DEFAULT_APPLICATION_ID)).thenReturn(List.of(rejectFeedback));
        when(vacancyApi.getVacancyCategory(DEFAULT_VACANCY_ID)).thenReturn(JobCategory.ENGINEERING);
        when(evaluationStrategy.supports(JobCategory.ENGINEERING)).thenReturn(true);
        when(evaluationStrategy.evaluate(List.of(rejectFeedback)))
                .thenReturn(anEvaluationResult(InterviewDecision.REJECT));

        assertThatThrownBy(() -> applicationService.updateStatus(DEFAULT_APPLICATION_ID, request))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot make an offer to candidate with REJECT evaluation");
    }

    @Test
    @DisplayName("updateStatus to OFFER with HIRE evaluation should update status, publish event, and return response")
    void givenHireEvaluation_updateStatus_shouldUpdateStatusToOfferAndReturnResponse() {
        Application existing = anApplication(ApplicationStatus.INTERVIEW);
        UpdateApplicationStatusRequest request = new UpdateApplicationStatusRequest(
                ApplicationStatus.OFFER,
                "Strong candidate"
        );
        InterviewFeedbackResponse hireFeedback = aHireFeedbackResponse();
        Instant updatedAt = DEFAULT_NOW.plusSeconds(3600);
        Application updatedApplication = anApplication(
                ApplicationStatus.OFFER,
                null,
                "Strong candidate",
                updatedAt
        );
        ApplicationResponse expectedResponse = anApplicationResponse(
                ApplicationStatus.OFFER,
                null,
                "Strong candidate",
                updatedAt
        );

        when(applicationRepository.findById(DEFAULT_APPLICATION_ID)).thenReturn(Optional.of(existing));
        when(feedbackRepository.findByApplicationId(DEFAULT_APPLICATION_ID)).thenReturn(List.of(hireFeedback));
        when(vacancyApi.getVacancyCategory(DEFAULT_VACANCY_ID)).thenReturn(JobCategory.ENGINEERING);
        when(evaluationStrategy.supports(JobCategory.ENGINEERING)).thenReturn(true);
        when(evaluationStrategy.evaluate(List.of(hireFeedback)))
                .thenReturn(anEvaluationResult(InterviewDecision.HIRE));
        when(commonGenerator.now()).thenReturn(updatedAt);
        when(applicationRepository.save(existing)).thenReturn(updatedApplication);
        when(applicationMapper.toResponse(updatedApplication)).thenReturn(expectedResponse);

        ApplicationResponse actual = applicationService.updateStatus(DEFAULT_APPLICATION_ID, request);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expectedResponse);
        verify(eventPublisher).publishEvent(new ApplicationStatusChangedEvent(
                DEFAULT_APPLICATION_ID,
                DEFAULT_VACANCY_ID,
                DEFAULT_CANDIDATE_ID,
                existing.getCandidateName(),
                existing.getEmail(),
                ApplicationStatus.INTERVIEW,
                ApplicationStatus.OFFER,
                "Strong candidate",
                updatedAt
        ));
    }

    @Test
    @DisplayName("updateStatus with non-existent id should throw ResourceNotFoundException")
    void givenNonExistentId_updateStatus_shouldThrowResourceNotFoundException() {
        UpdateApplicationStatusRequest request = new UpdateApplicationStatusRequest(
                ApplicationStatus.SCREENING,
                "Comment"
        );

        when(applicationRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.updateStatus(NON_EXISTENT_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Application not found with id: " + NON_EXISTENT_ID);
    }

    @Test
    @DisplayName("getApplicationsByVacancy with vacancyId should return mapped applications")
    void givenVacancyId_getApplicationsByVacancy_shouldReturnApplications() {
        Application application = anApplication();
        ApplicationResponse expectedResponse = anApplicationResponse();

        when(applicationRepository.findByVacancyId(DEFAULT_VACANCY_ID)).thenReturn(List.of(application));
        when(applicationMapper.toResponse(application)).thenReturn(expectedResponse);

        List<ApplicationResponse> actual = applicationService.getApplicationsByVacancy(DEFAULT_VACANCY_ID);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(List.of(expectedResponse));
    }

    @Test
    @DisplayName("submitFeedback with existing application in INTERVIEW status should save and return feedback")
    void givenExistingApplicationInInterview_submitFeedback_shouldSaveAndReturnResponse() {
        Application existing = anApplication(ApplicationStatus.INTERVIEW);
        SubmitInterviewFeedbackRequest request = validSubmitFeedbackRequest();
        InterviewFeedbackResponse expected = aFeedbackResponse();

        when(applicationRepository.findById(DEFAULT_APPLICATION_ID)).thenReturn(Optional.of(existing));
        when(commonGenerator.uuid()).thenReturn(DEFAULT_FEEDBACK_ID);
        when(commonGenerator.now()).thenReturn(DEFAULT_NOW);
        when(feedbackRepository.save(expected)).thenReturn(expected);

        InterviewFeedbackResponse actual = applicationService.submitFeedback(DEFAULT_APPLICATION_ID, request);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("submitFeedback when application not in INTERVIEW should throw InvalidStateTransitionException")
    void givenNonInterviewStatus_submitFeedback_shouldThrowInvalidStateTransitionException() {
        Application existing = anApplication(ApplicationStatus.APPLIED);
        SubmitInterviewFeedbackRequest request = validSubmitFeedbackRequest();

        when(applicationRepository.findById(DEFAULT_APPLICATION_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> applicationService.submitFeedback(DEFAULT_APPLICATION_ID, request))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot submit interview feedback for application "
                        + DEFAULT_APPLICATION_ID + " in status APPLIED");
    }

    @Test
    @DisplayName("getFeedbacks with existing application should return feedback list")
    void givenExistingApplication_getFeedbacks_shouldReturnList() {
        Application existing = anApplication(ApplicationStatus.INTERVIEW);
        InterviewFeedbackResponse feedback = aFeedbackResponse();

        when(applicationRepository.findById(DEFAULT_APPLICATION_ID)).thenReturn(Optional.of(existing));
        when(feedbackRepository.findByApplicationId(DEFAULT_APPLICATION_ID)).thenReturn(List.of(feedback));

        List<InterviewFeedbackResponse> actual = applicationService.getFeedbacks(DEFAULT_APPLICATION_ID);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(List.of(feedback));
    }

    @Test
    @DisplayName("evaluateCandidate with interview feedbacks should calculate score and return recommendation")
    void givenCandidateFeedbacks_evaluateCandidate_shouldExecuteStrategy() {
        Application application = anApplication(ApplicationStatus.INTERVIEW);
        InterviewFeedbackResponse fb1 = aFeedbackResponse(5);
        InterviewFeedbackResponse fb2 = aFeedbackResponse(
                UUID.fromString("00000000-0000-0000-0000-000000000004"),
                "Bob Lead",
                4,
                "Good skills"
        );
        EvaluationResult expectedResult = anEvaluationResult();

        when(applicationRepository.findById(DEFAULT_APPLICATION_ID)).thenReturn(Optional.of(application));
        when(feedbackRepository.findByApplicationId(DEFAULT_APPLICATION_ID)).thenReturn(List.of(fb1, fb2));
        when(vacancyApi.getVacancyCategory(DEFAULT_VACANCY_ID)).thenReturn(JobCategory.ENGINEERING);
        when(evaluationStrategy.supports(JobCategory.ENGINEERING)).thenReturn(true);
        when(evaluationStrategy.evaluate(List.of(fb1, fb2))).thenReturn(expectedResult);

        EvaluationResult actual = applicationService.evaluateCandidate(DEFAULT_APPLICATION_ID);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expectedResult);
    }

    @Test
    @DisplayName("evaluateCandidate with no matching strategy should throw IllegalStateException")
    void givenNoMatchingStrategy_evaluateCandidate_shouldThrowIllegalStateException() {
        Application application = anApplication(ApplicationStatus.INTERVIEW);
        InterviewFeedbackResponse fb = aFeedbackResponse();

        when(applicationRepository.findById(DEFAULT_APPLICATION_ID)).thenReturn(Optional.of(application));
        when(feedbackRepository.findByApplicationId(DEFAULT_APPLICATION_ID)).thenReturn(List.of(fb));
        when(vacancyApi.getVacancyCategory(DEFAULT_VACANCY_ID)).thenReturn(JobCategory.ENGINEERING);
        when(evaluationStrategy.supports(JobCategory.ENGINEERING)).thenReturn(false);

        assertThatThrownBy(() -> applicationService.evaluateCandidate(DEFAULT_APPLICATION_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No strategy found for category: " + JobCategory.ENGINEERING);
    }

    @Test
    @DisplayName("getApplicationsByVacancy with sortByScore true should sort by score descending with nulls last")
    void givenSortByScore_getApplicationsByVacancy_shouldReturnSortedList() {
        Application appLow = anApplicationBuilder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000011"))
                .matchingScore(50)
                .build();
        Application appHigh = anApplicationBuilder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000012"))
                .matchingScore(95)
                .build();
        Application appNull = anApplicationBuilder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000013"))
                .matchingScore(null)
                .build();

        ApplicationResponse resLow = anApplicationResponse(ApplicationStatus.APPLIED, 50, null, DEFAULT_NOW);
        ApplicationResponse resHigh = anApplicationResponse(ApplicationStatus.APPLIED, 95, null, DEFAULT_NOW);
        ApplicationResponse resNull = anApplicationResponse(ApplicationStatus.APPLIED, null, null, DEFAULT_NOW);

        when(applicationRepository.findByVacancyId(DEFAULT_VACANCY_ID)).thenReturn(List.of(appLow, appNull, appHigh));
        when(applicationMapper.toResponse(appLow)).thenReturn(resLow);
        when(applicationMapper.toResponse(appHigh)).thenReturn(resHigh);
        when(applicationMapper.toResponse(appNull)).thenReturn(resNull);

        List<ApplicationResponse> actual = applicationService.getApplicationsByVacancy(DEFAULT_VACANCY_ID, true);

        assertThat(actual).containsExactly(resHigh, resLow, resNull);
    }
}
