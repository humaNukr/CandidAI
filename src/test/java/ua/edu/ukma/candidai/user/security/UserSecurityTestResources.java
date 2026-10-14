package ua.edu.ukma.candidai.user.security;

import ua.edu.ukma.candidai.user.UserRole;
import ua.edu.ukma.candidai.user.model.User;

import java.time.Instant;
import java.util.UUID;

public final class UserSecurityTestResources {

    public static final String RAW_PASSWORD = "SecretPassword123!";
    public static final String WRONG_PASSWORD = "WrongPassword999!";
    public static final String TEST_EMAIL = "recruiter@candidai.com";
    public static final String MISSING_EMAIL = "missing@candidai.com";

    private UserSecurityTestResources() {
    }

    public static User testUser(String encodedPassword) {
        return User.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .fullName("Recruiter User")
                .email(TEST_EMAIL)
                .passwordHash(encodedPassword)
                .role(UserRole.RECRUITER)
                .createdAt(Instant.parse("2026-10-12T12:00:00Z"))
                .build();
    }
}
