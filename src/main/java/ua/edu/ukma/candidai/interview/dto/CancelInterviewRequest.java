package ua.edu.ukma.candidai.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for cancelling an interview")
public record CancelInterviewRequest(
        @Schema(description = "Reason for cancellation", example = "Candidate withdrew application")
        @NotBlank(message = "Cancellation reason must not be blank")
        @Size(max = 500, message = "Cancellation reason must not exceed 500 characters")
        String reason
) {
}
