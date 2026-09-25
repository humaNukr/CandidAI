package ua.edu.ukma.candidai.recruitment;

import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;

import java.time.Instant;
import java.util.UUID;

public record ApplicationStatusChangedEvent(
        UUID applicationId,
        UUID vacancyId,
        UUID candidateId,
        String candidateName,
        String email,
        ApplicationStatus previousStatus,
        ApplicationStatus newStatus,
        String comment,
        Instant changedAt
) {
    public ApplicationStatusChangedEvent(
            UUID applicationId,
            UUID vacancyId,
            UUID candidateId,
            String email,
            ApplicationStatus previousStatus,
            ApplicationStatus newStatus,
            String comment,
            Instant changedAt
    ) {
        this(applicationId, vacancyId, candidateId, null, email, previousStatus, newStatus, comment, changedAt);
    }

    public ApplicationStatusChangedEvent(
            UUID applicationId,
            UUID vacancyId,
            String candidateName,
            String email,
            ApplicationStatus previousStatus,
            ApplicationStatus newStatus,
            String comment,
            Instant changedAt
    ) {
        this(applicationId, vacancyId, null, candidateName, email, previousStatus, newStatus, comment, changedAt);
    }

    public ApplicationStatusChangedEvent(
            UUID applicationId,
            UUID vacancyId,
            String email,
            ApplicationStatus previousStatus,
            ApplicationStatus newStatus,
            String comment,
            Instant changedAt
    ) {
        this(applicationId, vacancyId, null, null, email, previousStatus, newStatus, comment, changedAt);
    }
}
