package ua.edu.ukma.candidai.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(description = "Request payload for rescheduling an interview")
public record RescheduleInterviewRequest(
        @Schema(description = "New scheduled interview start time", example = "2026-10-18T10:00:00Z")
        @NotNull(message = "newScheduledAt is required")
        @Future(message = "newScheduledAt must be in the future")
        Instant newScheduledAt,

        @Schema(description = "Duration in minutes", example = "45")
        @Positive(message = "durationMinutes must be positive")
        Integer durationMinutes,

        @Schema(description = "Updated meeting link", example = "https://meet.google.com/xyz-uvw-rst")
        @Size(max = 500, message = "meetingLink must not exceed 500 characters")
        String meetingLink,

        @Schema(description = "Reason for rescheduling", example = "Candidate requested time adjustment")
        @Size(max = 500, message = "reason must not exceed 500 characters")
        String reason
) {
}
