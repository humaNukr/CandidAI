package ua.edu.ukma.candidai.assessment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.edu.ukma.candidai.assessment.dto.AiScreeningResult;
import ua.edu.ukma.candidai.assessment.dto.ScreeningStatus;
import ua.edu.ukma.candidai.assessment.repository.ScreeningResultRepository;
import ua.edu.ukma.candidai.recruitment.ApplicationDetails;
import ua.edu.ukma.candidai.recruitment.RecruitmentApi;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.vacancy.VacancyApi;
import ua.edu.ukma.candidai.vacancy.VacancyDetails;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentServiceImpl implements AssessmentService {

    private final AiScreeningService aiScreeningService;
    private final ScreeningResultRepository screeningResultRepository;
    private final ResumeContentExtractor resumeContentExtractor;
    private final RecruitmentApi recruitmentApi;
    private final VacancyApi vacancyApi;

    @Override
    public AiScreeningResult executeScreening(UUID applicationId) {
        log.info("Executing AI screening for application: {}", applicationId);

        ApplicationDetails application = recruitmentApi.getApplication(applicationId);
        VacancyDetails vacancy = vacancyApi.getVacancyDetails(application.vacancyId());

        String resumeText = resumeContentExtractor.extractText(
                application.resumeUrl(),
                application.candidateName()
        );

        AiScreeningResult result = aiScreeningService.screenCandidate(applicationId, resumeText, vacancy);
        screeningResultRepository.save(result);

        if (result.status() == ScreeningStatus.FAILED) {
            log.warn("AI screening failed for application {}: {}. Status unchanged.",
                    applicationId, result.errorMessage());
            return result;
        }

        if (Boolean.TRUE.equals(result.passed())) {
            log.info("Application {} passed AI screening with score {}.",
                    applicationId, result.matchingScore());
            if (application.status() == ApplicationStatus.APPLIED) {
                recruitmentApi.updateStatus(
                        applicationId,
                        ApplicationStatus.SCREENING,
                        result.matchingScore(),
                        "AI screening passed with score " + result.matchingScore() + "/100"
                );
            }
        } else {
            log.info("Application {} rejected by AI screening with score {}.",
                    applicationId, result.matchingScore());
            if (application.status() != ApplicationStatus.REJECTED) {
                recruitmentApi.updateStatus(
                        applicationId,
                        ApplicationStatus.REJECTED,
                        result.matchingScore(),
                        result.summary()
                );
            }
        }

        return result;
    }
}
