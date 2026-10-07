package ua.edu.ukma.candidai.recruitment.service.strategy;

import io.swagger.v3.oas.annotations.media.Schema;
import ua.edu.ukma.candidai.recruitment.dto.model.InterviewDecision;

@Schema(description = "Aggregated candidate interview evaluation result")
public record EvaluationResult(
        @Schema(description = "Average technical evaluation score", example = "4.5")
        double averageScore,

        @Schema(description = "Recommended hiring decision", example = "HIRE")
        InterviewDecision recommendedDecision,

        @Schema(
                description = "Summary reason or decision criteria",
                example = "Candidate demonstrated strong architecture skills and cultural fit"
        )
        String summaryReason
) {}
