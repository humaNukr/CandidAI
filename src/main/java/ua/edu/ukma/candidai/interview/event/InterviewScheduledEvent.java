package ua.edu.ukma.candidai.interview.event;

import ua.edu.ukma.candidai.interview.model.InterviewType;

import java.time.Instant;
import java.util.UUID;

public record InterviewScheduledEvent(
        UUID interviewId,
        UUID applicationId,
        UUID interviewerId,
        String interviewerName,
        InterviewType type,
        Instant scheduledAt,
        int durationMinutes,
        String meetingLink
) {
}
