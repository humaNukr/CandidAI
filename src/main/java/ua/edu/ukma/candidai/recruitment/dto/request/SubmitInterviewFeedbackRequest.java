package ua.edu.ukma.candidai.recruitment.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ua.edu.ukma.candidai.recruitment.dto.model.InterviewDecision;

public record SubmitInterviewFeedbackRequest(
        @NotBlank(message = "Interviewer name is required")
        @Size(max = 100, message = "Interviewer name must not exceed 100 characters")
        String interviewerName,

        @NotNull(message = "Technical score is required")
        @Min(value = 1, message = "Technical score must be at least 1")
        @Max(value = 5, message = "Technical score must be at most 5")
        Integer technicalScore,

        @NotBlank(message = "Notes are required")
        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        String notes,

        @NotNull(message = "Decision is required")
        InterviewDecision decision
) {}
