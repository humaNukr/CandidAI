package ua.edu.ukma.candidai.recruitment.service.strategy;

import ua.edu.ukma.candidai.recruitment.dto.model.InterviewDecision;

public record EvaluationResult(
        double averageScore,
        InterviewDecision recommendedDecision,
        String summaryReason
) {}
