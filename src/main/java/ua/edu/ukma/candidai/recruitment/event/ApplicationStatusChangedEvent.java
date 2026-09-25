package ua.edu.ukma.candidai.recruitment.event;

import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;

import java.time.Instant;
import java.util.UUID;

public record ApplicationStatusChangedEvent(
        UUID applicationId,
        UUID vacancyId,
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
            String email,
            ApplicationStatus previousStatus,
            ApplicationStatus newStatus,
            String comment,
            Instant changedAt
    ) {
        this(applicationId, vacancyId, null, email, previousStatus, newStatus, comment, changedAt);
    }
}
