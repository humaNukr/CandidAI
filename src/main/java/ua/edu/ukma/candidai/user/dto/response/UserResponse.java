package ua.edu.ukma.candidai.user.dto.response;

import ua.edu.ukma.candidai.user.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String email,
        UserRole role,
        String telegramChatId,
        Instant createdAt
) {}
