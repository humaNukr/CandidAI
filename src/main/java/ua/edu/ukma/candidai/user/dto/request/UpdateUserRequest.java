package ua.edu.ukma.candidai.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Pattern(regexp = ".*\\S.*", message = "Full name must not be blank")
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        @Size(max = 50, message = "Telegram chat ID must not exceed 50 characters")
        String telegramChatId
) {}
