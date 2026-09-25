package ua.edu.ukma.candidai.recruitment.dto.response;

import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;

import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        UUID vacancyId,
        UUID candidateId,
        String candidateName,
        String email,
        String phone,
        String resumeUrl,
        ApplicationStatus status,
        String comment,
        Integer matchingScore,
        Instant appliedAt,
        Instant updatedAt
) {

    public ApplicationResponse(
            UUID id,
            UUID vacancyId,
            UUID candidateId,
            String candidateName,
            String email,
            String phone,
            String resumeUrl,
            ApplicationStatus status,
            String comment,
            Instant appliedAt,
            Instant updatedAt
    ) {
        this(
                id,
                vacancyId,
                candidateId,
                candidateName,
                email,
                phone,
                resumeUrl,
                status,
                comment,
                null,
                appliedAt,
                updatedAt
        );
    }
}
