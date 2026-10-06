package ua.edu.ukma.candidai.recruitment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import ua.edu.ukma.candidai.recruitment.dto.model.InterviewDecision;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Interview feedback response details")
public record InterviewFeedbackResponse(
        @Schema(description = "Unique feedback ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Associated application ID", example = "660e8400-e29b-41d4-a716-446655440000")
        UUID applicationId,

        @Schema(description = "Interviewer name", example = "Alex Smith")
        String interviewerName,

        @Schema(description = "Technical competency score (1 to 5)", example = "4")
        Integer technicalScore,

        @Schema(description = "Feedback notes", example = "Strong communication and algorithm skills.")
        String notes,

        @Schema(description = "Recommendation decision", example = "HIRE")
        InterviewDecision decision,

        @Schema(description = "Feedback timestamp", example = "2026-10-05T09:00:00Z")
        Instant createdAt
) {}
