package ua.edu.ukma.candidai.interview.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ua.edu.ukma.candidai.interview.model.InterviewType;

import java.time.Instant;
import java.util.UUID;

public record ScheduleInterviewRequest(
        @NotNull(message = "applicationId is required")
        UUID applicationId,

        UUID interviewerId,

        @NotBlank(message = "interviewerName is required")
        @Size(max = 120, message = "interviewerName must not exceed 120 characters")
        String interviewerName,

        @NotNull(message = "interview type is required")
        InterviewType type,

        @NotNull(message = "scheduledAt is required")
        @Future(message = "scheduledAt must be in the future")
        Instant scheduledAt,

        @Positive(message = "durationMinutes must be positive")
        Integer durationMinutes,

        @Size(max = 500, message = "meetingLink must not exceed 500 characters")
        String meetingLink,

        String notes
) {
}
