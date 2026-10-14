package ua.edu.ukma.candidai.common.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import ua.edu.ukma.candidai.common.security.dto.LoginRequest;

import java.util.List;

public final class SecurityTestResources {

    public static final String TEST_EMAIL = "recruiter@candidai.com";
    public static final String TEST_PASSWORD = "RecruiterPass123!";
    public static final String INVALID_PASSWORD = "WrongPassword!";
    public static final String TEST_ROLE = "ROLE_RECRUITER";
    public static final String ADMIN_EMAIL = "admin@candidai.com";
    public static final String ADMIN_PASSWORD = "AdminPass123!";
    public static final String TEST_SECRET
            = "candidai-super-secret-jwt-key-for-hmac-sha-256-which-is-at-least-256-bits-long!";
    public static final String TAMPERED_SECRET
            = "candidai-tampered-secret-jwt-key-different-for-testing-tampered-signatures!";
    public static final long TEST_EXPIRATION_MINUTES = 15L;

    private SecurityTestResources() {
    }

    public static UserDetails aTestUserDetails() {
        return User.builder()
                .username(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .authorities(List.of(new SimpleGrantedAuthority(TEST_ROLE)))
                .build();
    }

    public static UserDetails aTestUserDetails(String email, String role) {
        return User.builder()
                .username(email)
                .password(TEST_PASSWORD)
                .authorities(List.of(new SimpleGrantedAuthority(role)))
                .build();
    }

    public static LoginRequest aValidLoginRequest() {
        return new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
    }

    public static LoginRequest anAdminLoginRequest() {
        return new LoginRequest(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    public static LoginRequest anInvalidLoginRequest() {
        return new LoginRequest(TEST_EMAIL, INVALID_PASSWORD);
    }
}
