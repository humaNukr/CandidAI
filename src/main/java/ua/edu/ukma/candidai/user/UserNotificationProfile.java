package ua.edu.ukma.candidai.user;

import java.util.UUID;

public record UserNotificationProfile(
        UUID userId,
        String fullName,
        String email,
        String telegramChatId
) {}
