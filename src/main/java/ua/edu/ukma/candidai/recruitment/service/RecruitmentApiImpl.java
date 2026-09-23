package ua.edu.ukma.candidai.recruitment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ua.edu.ukma.candidai.recruitment.ApplicationDetails;
import ua.edu.ukma.candidai.recruitment.RecruitmentApi;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.request.UpdateApplicationStatusRequest;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class RecruitmentApiImpl implements RecruitmentApi {

    private final ApplicationService applicationService;

    @Override
    public ApplicationDetails getApplication(UUID applicationId) {
        ApplicationResponse app = applicationService.getById(applicationId);
        return new ApplicationDetails(
                app.id(),
                app.vacancyId(),
                app.candidateName(),
                app.email(),
                app.phone(),
                app.resumeUrl(),
                app.status(),
                app.comment()
        );
    }

    @Override
    public void updateStatus(UUID applicationId, ApplicationStatus status, String comment) {
        applicationService.updateStatus(applicationId, new UpdateApplicationStatusRequest(status, comment));
    }
}
