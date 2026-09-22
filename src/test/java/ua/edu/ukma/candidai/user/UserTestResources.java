package ua.edu.ukma.candidai.user;

import ua.edu.ukma.candidai.user.model.User;

import java.time.Instant;
import java.util.UUID;

public final class UserTestResources {

    public static final UUID DEFAULT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID SECOND_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    public static final UUID NON_EXISTENT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    public static final Instant DEFAULT_NOW = Instant.parse("2026-09-20T10:00:00Z");
    public static final String DEFAULT_FULL_NAME = "John Doe";
    public static final String DEFAULT_EMAIL = "john.doe@example.com";
    public static final String DEFAULT_PASSWORD_HASH = "hashed_password";
    public static final String DEFAULT_TELEGRAM_CHAT_ID = "123456789";
    public static final String UPDATED_TELEGRAM_CHAT_ID = "987654321";

    private UserTestResources() {
    }

    public static String expectedUserNotFoundMessage(UUID userId) {
        return "User not found with id: " + userId;
    }

    public static User.UserBuilder sampleUserBuilder() {
        return User.builder()
                .id(DEFAULT_USER_ID)
                .fullName(DEFAULT_FULL_NAME)
                .email(DEFAULT_EMAIL)
                .passwordHash(DEFAULT_PASSWORD_HASH)
                .role(UserRole.RECRUITER)
                .telegramChatId(DEFAULT_TELEGRAM_CHAT_ID)
                .createdAt(DEFAULT_NOW);
    }

    public static User sampleUser() {
        return sampleUserBuilder().build();
    }

    public static User sampleUserWithTelegramChatId(String telegramChatId) {
        return sampleUserBuilder()
                .telegramChatId(telegramChatId)
                .build();
    }

    public static UserNotificationProfile sampleUserNotificationProfile() {
        return new UserNotificationProfile(
                DEFAULT_USER_ID,
                DEFAULT_FULL_NAME,
                DEFAULT_EMAIL,
                DEFAULT_TELEGRAM_CHAT_ID
        );
    }
}
