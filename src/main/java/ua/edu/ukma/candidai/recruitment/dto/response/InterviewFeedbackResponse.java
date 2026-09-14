package ua.edu.ukma.candidai.recruitment.dto.response;

import ua.edu.ukma.candidai.recruitment.dto.model.InterviewDecision;

import java.time.Instant;
import java.util.UUID;

public record InterviewFeedbackResponse(
        UUID id,
        UUID applicationId,
        String interviewerName,
        Integer technicalScore,
        String notes,
        InterviewDecision decision,
        Instant createdAt
) {}
