package ua.edu.ukma.candidai.recruitment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.model.InterviewDecision;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static ua.edu.ukma.candidai.recruitment.RecruitmentTestResources.*;

@ExtendWith(MockitoExtension.class)
class CandidateEvaluationServiceImplTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private InterviewFeedbackRepository feedbackRepository;

    @Mock
    private VacancyApi vacancyApi;

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private CandidateEvaluationStrategy evaluationStrategy;

    private CandidateEvaluationServiceImpl candidateEvaluationService;

    @BeforeEach
    void setUp() {
        candidateEvaluationService = new CandidateEvaluationServiceImpl(
                applicationRepository,
                feedbackRepository,
                vacancyApi,
                applicationMapper,
                List.of(evaluationStrategy)
        );
    }

    @Test
    @DisplayName("evaluateCandidate with interview feedbacks should calculate score and return recommendation")
    void givenCandidateFeedbacks_evaluateCandidate_shouldExecuteStrategy() {
        Application application = anApplication(ApplicationStatus.INTERVIEW);
        InterviewFeedback fbEntity1 = anInterviewFeedback(application, 5, InterviewDecision.HIRE);
        InterviewFeedback fbEntity2 = anInterviewFeedback(application, 4, InterviewDecision.HIRE);
        InterviewFeedbackResponse fb1 = aFeedbackResponse(5);
        InterviewFeedbackResponse fb2 = aFeedbackResponse(
                UUID.fromString("00000000-0000-0000-0000-000000000004"),
                "Bob Lead",
                4,
                "Good skills"
        );
        EvaluationResult expectedResult = anEvaluationResult();

        when(applicationRepository.findById(DEFAULT_APPLICATION_ID)).thenReturn(Optional.of(application));
        when(feedbackRepository.findByApplicationId(DEFAULT_APPLICATION_ID)).thenReturn(List.of(fbEntity1, fbEntity2));
        when(applicationMapper.toFeedbackResponseList(List.of(fbEntity1, fbEntity2))).thenReturn(List.of(fb1, fb2));
        when(vacancyApi.getVacancyCategory(DEFAULT_VACANCY_ID)).thenReturn(JobCategory.ENGINEERING);
        when(evaluationStrategy.supports(JobCategory.ENGINEERING)).thenReturn(true);
        when(evaluationStrategy.evaluate(List.of(fb1, fb2))).thenReturn(expectedResult);

        EvaluationResult actual = candidateEvaluationService.evaluateCandidate(DEFAULT_APPLICATION_ID);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expectedResult);
    }

    @Test
    @DisplayName("evaluateCandidate with no matching strategy should throw IllegalStateException")
    void givenNoMatchingStrategy_evaluateCandidate_shouldThrowIllegalStateException() {
        Application application = anApplication(ApplicationStatus.INTERVIEW);
        InterviewFeedback fbEntity = anInterviewFeedback(application);
        InterviewFeedbackResponse fb = aFeedbackResponse();

        when(applicationRepository.findById(DEFAULT_APPLICATION_ID)).thenReturn(Optional.of(application));
        when(feedbackRepository.findByApplicationId(DEFAULT_APPLICATION_ID)).thenReturn(List.of(fbEntity));
        when(applicationMapper.toFeedbackResponseList(List.of(fbEntity))).thenReturn(List.of(fb));
        when(vacancyApi.getVacancyCategory(DEFAULT_VACANCY_ID)).thenReturn(JobCategory.ENGINEERING);
        when(evaluationStrategy.supports(JobCategory.ENGINEERING)).thenReturn(false);

        assertThatThrownBy(() -> candidateEvaluationService.evaluateCandidate(DEFAULT_APPLICATION_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No strategy found for category: " + JobCategory.ENGINEERING);
    }

    @Test
    @DisplayName("evaluateCandidate when application not found should throw ResourceNotFoundException")
    void givenMissingApplication_evaluateCandidate_shouldThrowResourceNotFoundException() {
        when(applicationRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidateEvaluationService.evaluateCandidate(NON_EXISTENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Application not found with id: " + NON_EXISTENT_ID);
    }
}
