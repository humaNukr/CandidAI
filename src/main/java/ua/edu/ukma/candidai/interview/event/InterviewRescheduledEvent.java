package ua.edu.ukma.candidai.interview.event;

import java.time.Instant;
import java.util.UUID;

public record InterviewRescheduledEvent(
        UUID interviewId,
        UUID applicationId,
        Instant newScheduledAt,
        int durationMinutes,
        String meetingLink,
        String reason
) {
}
