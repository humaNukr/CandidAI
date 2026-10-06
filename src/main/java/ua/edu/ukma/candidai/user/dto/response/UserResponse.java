package ua.edu.ukma.candidai.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import ua.edu.ukma.candidai.user.UserRole;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "User account response payload")
public record UserResponse(
        @Schema(description = "Unique user ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Full user name", example = "Jane Doe")
        String fullName,

        @Schema(description = "User email address", example = "jane.doe@example.com")
        String email,

        @Schema(description = "User role", example = "CANDIDATE")
        UserRole role,

        @Schema(description = "Connected Telegram Chat ID", example = "123456789")
        String telegramChatId,

        @Schema(description = "Account registration timestamp", example = "2026-10-05T08:00:00Z")
        Instant createdAt
) {}
