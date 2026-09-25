package ua.edu.ukma.candidai.user.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        @Size(max = 50, message = "Telegram chat ID must not exceed 50 characters")
        String telegramChatId
) {}
