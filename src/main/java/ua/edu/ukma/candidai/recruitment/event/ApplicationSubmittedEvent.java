package ua.edu.ukma.candidai.recruitment.event;

import java.time.Instant;
import java.util.UUID;

public record ApplicationSubmittedEvent(
        UUID applicationId,
        UUID vacancyId,
        String candidateName,
        String email,
        String resumeUrl,
        Instant submittedAt
) {
}
