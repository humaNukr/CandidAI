package ua.edu.ukma.candidai.recruitment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ua.edu.ukma.candidai.recruitment.dto.model.InterviewDecision;

@Schema(description = "Request payload for submitting interview feedback")
public record SubmitInterviewFeedbackRequest(
        @Schema(description = "Name of the interviewer", example = "Alex Smith")
        @NotBlank(message = "Interviewer name is required")
        @Size(max = 100, message = "Interviewer name must not exceed 100 characters")
        String interviewerName,

        @Schema(description = "Technical competency score (1 to 5)", example = "4")
        @NotNull(message = "Technical score is required")
        @Min(value = 1, message = "Technical score must be at least 1")
        @Max(value = 5, message = "Technical score must be at most 5")
        Integer technicalScore,

        @Schema(description = "Detailed interview notes", example = "Strong algorithms and system design skills.")
        @NotBlank(message = "Notes are required")
        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        String notes,

        @Schema(description = "Hiring decision recommendation", example = "HIRE")
        @NotNull(message = "Decision is required")
        InterviewDecision decision
) {}
