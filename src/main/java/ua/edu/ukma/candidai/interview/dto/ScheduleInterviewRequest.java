package ua.edu.ukma.candidai.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ua.edu.ukma.candidai.interview.model.InterviewType;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Request payload for scheduling an interview")
public record ScheduleInterviewRequest(
        @Schema(description = "Application ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull(message = "applicationId is required")
        UUID applicationId,

        @Schema(description = "Interviewer user ID", example = "a0000000-0000-0000-0000-000000000001")
        UUID interviewerId,

        @Schema(description = "Interviewer full name", example = "Alex Smith")
        @NotBlank(message = "interviewerName is required")
        @Size(max = 120, message = "interviewerName must not exceed 120 characters")
        String interviewerName,

        @Schema(description = "Type of interview", example = "TECHNICAL")
        @NotNull(message = "interview type is required")
        InterviewType type,

        @Schema(description = "Scheduled interview start timestamp", example = "2026-10-15T14:00:00Z")
        @NotNull(message = "scheduledAt is required")
        @Future(message = "scheduledAt must be in the future")
        Instant scheduledAt,

        @Schema(description = "Duration in minutes", example = "60")
        @Positive(message = "durationMinutes must be positive")
        Integer durationMinutes,

        @Schema(description = "Online meeting link", example = "https://meet.google.com/abc-defg-hij")
        @Size(max = 500, message = "meetingLink must not exceed 500 characters")
        String meetingLink,

        @Schema(description = "Interview preparation notes", example = "Prepare live coding tasks on Spring Boot")
        String notes
) {
}
