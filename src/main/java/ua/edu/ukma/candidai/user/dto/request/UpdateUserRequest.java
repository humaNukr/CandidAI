package ua.edu.ukma.candidai.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for updating user profile")
public record UpdateUserRequest(
        @Schema(description = "Updated full name", example = "Jane Doe Updated")
        @Pattern(regexp = ".*\\S.*", message = "Full name must not be blank")
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        @Schema(description = "Updated Telegram Chat ID", example = "987654321")
        @Size(max = 50, message = "Telegram chat ID must not exceed 50 characters")
        String telegramChatId
) {}
