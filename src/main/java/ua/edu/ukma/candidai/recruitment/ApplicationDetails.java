package ua.edu.ukma.candidai.recruitment;

import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;

import java.util.UUID;

public record ApplicationDetails(
        UUID id,
        UUID vacancyId,
        String candidateName,
        String email,
        String phone,
        String resumeUrl,
        ApplicationStatus status,
        String comment,
        Integer matchingScore
) {
    public ApplicationDetails(
            UUID id,
            UUID vacancyId,
            String candidateName,
            String email,
            String phone,
            String resumeUrl,
            ApplicationStatus status,
            String comment
    ) {
        this(id, vacancyId, candidateName, email, phone, resumeUrl, status, comment, null);
    }
}
