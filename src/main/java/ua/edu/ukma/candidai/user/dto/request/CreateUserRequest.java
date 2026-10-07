package ua.edu.ukma.candidai.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ua.edu.ukma.candidai.user.UserRole;

@Schema(description = "Request payload for creating a new user")
public record CreateUserRequest(
        @Schema(description = "Full user name", example = "Jane Doe")
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        @Schema(description = "User email address", example = "jane.doe@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @Schema(description = "Account password (min 8 chars)", example = "SecurePass123!")
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        @Schema(description = "User role", example = "CANDIDATE")
        @NotNull(message = "Role is required")
        UserRole role,

        @Schema(description = "Telegram Chat ID for notifications", example = "123456789")
        @Size(max = 50, message = "Telegram chat ID must not exceed 50 characters")
        String telegramChatId
) {}
