package ua.edu.ukma.candidai.recruitment;

import java.time.Instant;
import java.util.UUID;

public record ApplicationSubmittedEvent(
        UUID applicationId,
        UUID vacancyId,
        UUID candidateId,
        String candidateName,
        String email,
        String resumeUrl,
        Instant submittedAt
) {
}
