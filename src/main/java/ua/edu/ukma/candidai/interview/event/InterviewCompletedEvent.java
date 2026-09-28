package ua.edu.ukma.candidai.interview.event;

import java.util.UUID;

public record InterviewCompletedEvent(
        UUID interviewId,
        UUID applicationId
) {
}
