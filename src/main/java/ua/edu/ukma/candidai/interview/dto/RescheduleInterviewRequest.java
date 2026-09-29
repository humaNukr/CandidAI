package ua.edu.ukma.candidai.interview.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record RescheduleInterviewRequest(
        @NotNull(message = "newScheduledAt is required")
        @Future(message = "newScheduledAt must be in the future")
        Instant newScheduledAt,

        @Positive(message = "durationMinutes must be positive")
        Integer durationMinutes,

        @Size(max = 500, message = "meetingLink must not exceed 500 characters")
        String meetingLink,

        @Size(max = 500, message = "reason must not exceed 500 characters")
        String reason
) {
}
