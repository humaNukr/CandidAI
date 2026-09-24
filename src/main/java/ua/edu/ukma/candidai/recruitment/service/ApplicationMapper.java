package ua.edu.ukma.candidai.recruitment.service;

import org.springframework.stereotype.Component;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;
import ua.edu.ukma.candidai.recruitment.model.Application;

@Component
public class ApplicationMapper {

    public ApplicationResponse toResponse(Application application) {
        if (application == null) {
            return null;
        }

        return new ApplicationResponse(
                application.getId(),
                application.getVacancyId(),
                application.getCandidateName(),
                application.getEmail(),
                application.getPhone(),
                application.getResumeUrl(),
                application.getStatus(),
                application.getComment(),
                application.getMatchingScore(),
                application.getAppliedAt(),
                application.getUpdatedAt()
        );
    }
}
