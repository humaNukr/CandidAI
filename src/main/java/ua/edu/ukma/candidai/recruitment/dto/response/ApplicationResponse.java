package ua.edu.ukma.candidai.recruitment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Job application details response")
public record ApplicationResponse(
        @Schema(description = "Unique application ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Associated vacancy ID", example = "a0000000-0000-0000-0000-000000000001")
        UUID vacancyId,

        @Schema(description = "Candidate ID", example = "c0000000-0000-0000-0000-000000000001")
        UUID candidateId,

        @Schema(description = "Candidate full name", example = "Jane Doe")
        String candidateName,

        @Schema(description = "Candidate contact email", example = "jane.doe@example.com")
        String email,

        @Schema(description = "Candidate phone number", example = "+380501234567")
        String phone,

        @Schema(description = "Resume storage URL", example = "https://storage.candidai.ukma.edu.ua/resumes/jane_doe.pdf")
        String resumeUrl,

        @Schema(description = "Current application status", example = "APPLIED")
        ApplicationStatus status,

        @Schema(description = "AI screening matching score (0-100)", example = "85")
        Integer matchingScore,

        @Schema(description = "Recruiter comment or evaluation note", example = "Strong profile")
        String comment,

        @Schema(description = "Application submission timestamp", example = "2026-10-05T08:00:00Z")
        Instant appliedAt,

        @Schema(description = "Last update timestamp", example = "2026-10-05T08:30:00Z")
        Instant updatedAt
) {
}
