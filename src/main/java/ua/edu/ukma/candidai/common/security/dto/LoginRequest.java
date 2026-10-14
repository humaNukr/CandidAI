package ua.edu.ukma.candidai.common.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credentials for user authentication")
public record LoginRequest(
        @Schema(description = "User registered email address", example = "recruiter@candidai.com")
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be a valid email address")
        String email,

        @Schema(description = "Raw user password", example = "RecruiterPass123!")
        @NotBlank(message = "Password must not be blank")
        String password
) {
}
