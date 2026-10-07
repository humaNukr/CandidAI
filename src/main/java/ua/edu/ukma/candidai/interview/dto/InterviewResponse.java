package ua.edu.ukma.candidai.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ua.edu.ukma.candidai.interview.model.Interview;
import ua.edu.ukma.candidai.interview.model.InterviewStatus;
import ua.edu.ukma.candidai.interview.model.InterviewType;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Interview details response payload")
public record InterviewResponse(
        @Schema(description = "Unique interview ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Associated application ID", example = "660e8400-e29b-41d4-a716-446655440000")
        UUID applicationId,

        @Schema(description = "Interviewer user ID", example = "a0000000-0000-0000-0000-000000000001")
        UUID interviewerId,

        @Schema(description = "Interviewer full name", example = "Alex Smith")
        String interviewerName,

        @Schema(description = "Type of interview", example = "TECHNICAL")
        InterviewType type,

        @Schema(description = "Interview status", example = "SCHEDULED")
        InterviewStatus status,

        @Schema(description = "Scheduled start time", example = "2026-10-15T14:00:00Z")
        Instant scheduledAt,

        @Schema(description = "Duration in minutes", example = "60")
        int durationMinutes,

        @Schema(description = "Meeting video call link", example = "https://meet.google.com/abc-defg-hij")
        String meetingLink,

        @Schema(description = "Interview notes", example = "Technical screening session")
        String notes,

        @Schema(description = "Cancellation reason if cancelled", example = "null")
        String cancellationReason,

        @Schema(description = "Created timestamp", example = "2026-10-05T08:00:00Z")
        Instant createdAt,

        @Schema(description = "Updated timestamp", example = "2026-10-05T08:30:00Z")
        Instant updatedAt
) {
    public static InterviewResponse from(Interview interview) {
        return new InterviewResponse(
                interview.getId(),
                interview.getApplicationId(),
                interview.getInterviewerId(),
                interview.getInterviewerName(),
                interview.getType(),
                interview.getStatus(),
                interview.getScheduledAt(),
                interview.getDurationMinutes(),
                interview.getMeetingLink(),
                interview.getNotes(),
                interview.getCancellationReason(),
                interview.getCreatedAt(),
                interview.getUpdatedAt()
        );
    }
}
