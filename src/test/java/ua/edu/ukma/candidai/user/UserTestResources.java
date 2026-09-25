package ua.edu.ukma.candidai.user;

import ua.edu.ukma.candidai.user.dto.request.CreateUserRequest;
import ua.edu.ukma.candidai.user.dto.request.UpdateUserRequest;
import ua.edu.ukma.candidai.user.dto.response.UserResponse;
import ua.edu.ukma.candidai.user.model.User;

import java.time.Instant;
import java.util.UUID;

public final class UserTestResources {

    public static final String BASE_URL = "/api/v1/users";
    public static final UUID DEFAULT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID SECOND_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    public static final UUID NON_EXISTENT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    public static final Instant DEFAULT_NOW = Instant.parse("2026-09-20T10:00:00Z");
    public static final String DEFAULT_FULL_NAME = "John Doe";
    public static final String DEFAULT_EMAIL = "john.doe@example.com";
    public static final String DEFAULT_PASSWORD = "password123";
    public static final String DEFAULT_PASSWORD_HASH = "hashed_password";
    public static final UserRole DEFAULT_ROLE = UserRole.RECRUITER;
    public static final String DEFAULT_TELEGRAM_CHAT_ID = "123456789";
    public static final String UPDATED_FULL_NAME = "Jane Doe";
    public static final String UPDATED_TELEGRAM_CHAT_ID = "987654321";

    public static final String JSON_WITH_UNKNOWN_PROPERTY = """
            {
                "unknownField": "bad"
            }
            """;

    public static final String VALIDATION_ERROR_JSON = """
            {
                "type": "https://candidai.ukma.edu.ua/errors/validation",
                "title": "Validation Error",
                "status": 400,
                "detail": "Input validation failed"
            }
            """;

    public static final String JSON_PARSING_ERROR_JSON = """
            {
                "type": "https://candidai.ukma.edu.ua/errors/bad-request",
                "title": "JSON Parsing Error",
                "status": 400,
                "detail": "Malformed request body or unknown properties"
            }
            """;

    private UserTestResources() {
    }

    public static String expectedUserNotFoundMessage(UUID userId) {
        return "User not found with id: " + userId;
    }

    public static String expectedDuplicateEmailMessage(String email) {
        return "User with email " + email + " already exists";
    }

    public static String notFoundProblemDetailJson(UUID id) {
        return """
                {
                    "type": "https://candidai.ukma.edu.ua/errors/not-found",
                    "title": "Resource Not Found",
                    "status": 404,
                    "detail": "%s"
                }
                """.formatted(expectedUserNotFoundMessage(id));
    }

    public static String conflictProblemDetailJson(String email) {
        return """
                {
                    "type": "https://candidai.ukma.edu.ua/errors/conflict",
                    "title": "Resource Conflict",
                    "status": 409,
                    "detail": "%s"
                }
                """.formatted(expectedDuplicateEmailMessage(email));
    }

    public static CreateUserRequest sampleCreateUserRequest() {
        return new CreateUserRequest(
                DEFAULT_FULL_NAME,
                DEFAULT_EMAIL,
                DEFAULT_PASSWORD,
                DEFAULT_ROLE,
                DEFAULT_TELEGRAM_CHAT_ID
        );
    }

    public static CreateUserRequest sampleCreateUserRequestWithoutTelegramChatId() {
        return new CreateUserRequest(
                DEFAULT_FULL_NAME,
                DEFAULT_EMAIL,
                DEFAULT_PASSWORD,
                DEFAULT_ROLE,
                null
        );
    }

    public static UpdateUserRequest sampleUpdateUserRequest() {
        return new UpdateUserRequest(
                UPDATED_FULL_NAME,
                UPDATED_TELEGRAM_CHAT_ID
        );
    }

    public static UserResponse sampleUserResponse() {
        return new UserResponse(
                DEFAULT_USER_ID,
                DEFAULT_FULL_NAME,
                DEFAULT_EMAIL,
                DEFAULT_ROLE,
                DEFAULT_TELEGRAM_CHAT_ID,
                DEFAULT_NOW
        );
    }

    public static UserResponse sampleUserResponse(UUID id, String email, UserRole role) {
        return new UserResponse(
                id,
                DEFAULT_FULL_NAME,
                email,
                role,
                DEFAULT_TELEGRAM_CHAT_ID,
                DEFAULT_NOW
        );
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
