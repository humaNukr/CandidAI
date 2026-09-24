package ua.edu.ukma.candidai.recruitment;

import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;

import java.util.UUID;

public interface RecruitmentApi {

    ApplicationDetails getApplication(UUID applicationId);

    void updateStatus(UUID applicationId, ApplicationStatus status, Integer matchingScore, String comment);

    default void updateStatus(UUID applicationId, ApplicationStatus status, String comment) {
        updateStatus(applicationId, status, null, comment);
    }
}
