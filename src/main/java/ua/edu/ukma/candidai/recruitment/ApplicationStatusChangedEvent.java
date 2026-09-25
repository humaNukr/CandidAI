package ua.edu.ukma.candidai.recruitment;

import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;

import java.time.Instant;
import java.util.UUID;

public record ApplicationStatusChangedEvent(
        UUID applicationId,
        UUID vacancyId,
        UUID candidateId,
        String email,
        ApplicationStatus previousStatus,
        ApplicationStatus newStatus,
        String comment,
        Instant changedAt
) {
}
