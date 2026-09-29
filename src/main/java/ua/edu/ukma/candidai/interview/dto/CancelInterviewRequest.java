package ua.edu.ukma.candidai.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelInterviewRequest(
        @NotBlank(message = "Cancellation reason must not be blank")
        @Size(max = 500, message = "Cancellation reason must not exceed 500 characters")
        String reason
) {
}
