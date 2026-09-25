package ua.edu.ukma.candidai.assessment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.edu.ukma.candidai.assessment.dto.AiScreeningResult;
import ua.edu.ukma.candidai.assessment.repository.ScreeningResultRepository;
import ua.edu.ukma.candidai.recruitment.ApplicationDetails;
import ua.edu.ukma.candidai.recruitment.RecruitmentApi;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.vacancy.VacancyApi;
import ua.edu.ukma.candidai.vacancy.VacancyDetails;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceImplTest {

    private static final UUID APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID VACANCY_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final Instant NOW = Instant.parse("2026-09-21T10:00:00Z");

    @Mock
    private AiScreeningService aiScreeningService;

    @Mock
    private ScreeningResultRepository screeningResultRepository;

    @Mock
    private ResumeContentExtractor resumeContentExtractor;

    @Mock
    private RecruitmentApi recruitmentApi;

    @Mock
    private VacancyApi vacancyApi;

    @InjectMocks
    private AssessmentServiceImpl assessmentService;

    @Test
    @DisplayName("executeScreening - should move status to SCREENING when candidate passes")
    void givenPassingScreening_executeScreening_shouldUpdateStatusToScreening() {
        ApplicationDetails app = sampleApplication();
        VacancyDetails vacancy = sampleVacancy();
        AiScreeningResult result = AiScreeningResult.completed(
                APP_ID, VACANCY_ID, 85, true, "Strong fit", List.of("Java"), List.of(), List.of("Q1"), NOW
        );

        when(recruitmentApi.getApplication(APP_ID)).thenReturn(app);
        when(vacancyApi.getVacancyDetails(VACANCY_ID)).thenReturn(vacancy);
        when(resumeContentExtractor.extractText(any(), any())).thenReturn("Java resume text");
        when(aiScreeningService.screenCandidate(eq(APP_ID), any(), eq(vacancy))).thenReturn(result);

        AiScreeningResult actual = assessmentService.executeScreening(APP_ID);

        assertThat(actual).isEqualTo(result);
        verify(screeningResultRepository).save(result);
        verify(recruitmentApi).updateStatus(eq(APP_ID), eq(ApplicationStatus.SCREENING), eq(85), contains("85"));
    }

    @Test
    @DisplayName("executeScreening - should move status to REJECTED when candidate fails")
    void givenFailingScreening_executeScreening_shouldUpdateStatusToRejected() {
        ApplicationDetails app = sampleApplication();
        VacancyDetails vacancy = sampleVacancy();
        AiScreeningResult result = AiScreeningResult.completed(
                APP_ID, VACANCY_ID, 30, false, "Lacks Java knowledge", List.of(), List.of("Java"), List.of(), NOW
        );

        when(recruitmentApi.getApplication(APP_ID)).thenReturn(app);
        when(vacancyApi.getVacancyDetails(VACANCY_ID)).thenReturn(vacancy);
        when(resumeContentExtractor.extractText(any(), any())).thenReturn("Non-technical resume");
        when(aiScreeningService.screenCandidate(eq(APP_ID), any(), eq(vacancy))).thenReturn(result);

        AiScreeningResult actual = assessmentService.executeScreening(APP_ID);

        assertThat(actual).isEqualTo(result);
        verify(screeningResultRepository).save(result);
        verify(recruitmentApi).updateStatus(
                eq(APP_ID),
                eq(ApplicationStatus.REJECTED),
                eq(30),
                eq("Lacks Java knowledge")
        );
    }

    @Test
    @DisplayName("executeScreening - should not change status when AI screening fails")
    void givenFailedScreening_executeScreening_shouldNotUpdateStatus() {
        ApplicationDetails app = sampleApplication();
        VacancyDetails vacancy = sampleVacancy();
        AiScreeningResult result = AiScreeningResult.failed(
                APP_ID, VACANCY_ID, "Gemini API unavailable", NOW
        );

        when(recruitmentApi.getApplication(APP_ID)).thenReturn(app);
        when(vacancyApi.getVacancyDetails(VACANCY_ID)).thenReturn(vacancy);
        when(resumeContentExtractor.extractText(any(), any())).thenReturn("Resume text");
        when(aiScreeningService.screenCandidate(eq(APP_ID), any(), eq(vacancy))).thenReturn(result);

        AiScreeningResult actual = assessmentService.executeScreening(APP_ID);

        assertThat(actual).isEqualTo(result);
        verify(screeningResultRepository).save(result);
        verify(recruitmentApi, never()).updateStatus(any(), any(), any(), any());
    }

    @Test
    @DisplayName("executeScreening - should not update status if candidate is already in SCREENING status")
    void givenPassingScreeningWhenAlreadyScreening_executeScreening_shouldNotUpdateStatus() {
        ApplicationDetails app = new ApplicationDetails(
                APP_ID, VACANCY_ID, "Jane Doe", "test@example.com", "+380501112233",
                "https://storage.candidai.ukma.edu.ua/resumes/jane.pdf", ApplicationStatus.SCREENING, null
        );
        VacancyDetails vacancy = sampleVacancy();
        AiScreeningResult result = AiScreeningResult.completed(
                APP_ID, VACANCY_ID, 85, true, "Strong fit", List.of("Java"), List.of(), List.of("Q1"), NOW
        );

        when(recruitmentApi.getApplication(APP_ID)).thenReturn(app);
        when(vacancyApi.getVacancyDetails(VACANCY_ID)).thenReturn(vacancy);
        when(resumeContentExtractor.extractText(any(), any())).thenReturn("Java resume text");
        when(aiScreeningService.screenCandidate(eq(APP_ID), any(), eq(vacancy))).thenReturn(result);

        AiScreeningResult actual = assessmentService.executeScreening(APP_ID);

        assertThat(actual).isEqualTo(result);
        verify(screeningResultRepository).save(result);
        verify(recruitmentApi, never()).updateStatus(any(), any(), any(), any());
    }

    private ApplicationDetails sampleApplication() {
        return new ApplicationDetails(
                APP_ID, VACANCY_ID, "Jane Doe", "test@example.com", "+380501112233",
                "https://storage.candidai.ukma.edu.ua/resumes/jane.pdf", ApplicationStatus.APPLIED, null
        );
    }

    private VacancyDetails sampleVacancy() {
        return new VacancyDetails(
                VACANCY_ID, "Java Dev", "Backend", List.of("Java"), List.of(), "Mid"
        );
    }
}
