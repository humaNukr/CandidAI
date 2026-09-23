package ua.edu.ukma.candidai.user;

import java.util.Optional;
import java.util.UUID;

public interface UserApi {

    Optional<UserNotificationProfile> getUserNotificationProfile(UUID userId);

    void linkTelegramChatId(UUID userId, String telegramChatId);
}
