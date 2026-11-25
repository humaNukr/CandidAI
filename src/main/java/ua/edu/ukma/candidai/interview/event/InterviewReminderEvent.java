package ua.edu.ukma.candidai.interview.event;

import java.time.Instant;
import java.util.UUID;
import ua.edu.ukma.candidai.interview.model.InterviewType;

public record InterviewReminderEvent(
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
