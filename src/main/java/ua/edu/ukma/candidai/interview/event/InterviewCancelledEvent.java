package ua.edu.ukma.candidai.interview.event;

import java.util.UUID;

public record InterviewCancelledEvent(
        UUID interviewId,
        UUID applicationId,
        String reason
) {
}
