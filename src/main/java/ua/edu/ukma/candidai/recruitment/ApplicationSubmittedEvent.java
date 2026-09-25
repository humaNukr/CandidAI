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
    public ApplicationSubmittedEvent(
            UUID applicationId,
            UUID vacancyId,
            String candidateName,
            String email,
            String resumeUrl,
            Instant submittedAt
    ) {
        this(applicationId, vacancyId, null, candidateName, email, resumeUrl, submittedAt);
    }
}
