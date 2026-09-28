package ua.edu.ukma.candidai.interview.dto;

import ua.edu.ukma.candidai.interview.model.Interview;
import ua.edu.ukma.candidai.interview.model.InterviewStatus;
import ua.edu.ukma.candidai.interview.model.InterviewType;

import java.time.Instant;
import java.util.UUID;

public record InterviewResponse(
        UUID id,
        UUID applicationId,
        UUID interviewerId,
        String interviewerName,
        InterviewType type,
        InterviewStatus status,
        Instant scheduledAt,
        int durationMinutes,
        String meetingLink,
        String notes,
        String cancellationReason,
        Instant createdAt,
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
