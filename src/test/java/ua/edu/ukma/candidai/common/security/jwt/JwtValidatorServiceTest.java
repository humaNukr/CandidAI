package ua.edu.ukma.candidai.common.security.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;
import static ua.edu.ukma.candidai.common.security.SecurityTestResources.*;

class JwtValidatorServiceTest {

    private final JwtService jwtService = new JwtService(TEST_SECRET, TEST_EXPIRATION_MINUTES);
    private final JwtValidatorService jwtValidatorService = new JwtValidatorService(TEST_SECRET);
    private final JwtService tamperedJwtService = new JwtService(TAMPERED_SECRET, TEST_EXPIRATION_MINUTES);

    @Test
    @DisplayName("extractUsername should return correct username from valid token")
    void givenValidToken_extractUsername_shouldReturnSubject() {
        UserDetails userDetails = aTestUserDetails();
        String token = jwtService.generateToken(userDetails);

        String username = jwtValidatorService.extractUsername(token);

        assertThat(username).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("isTokenValid should return true for valid token and matching user")
    void givenValidTokenAndMatchingUser_isTokenValid_shouldReturnTrue() {
        UserDetails userDetails = aTestUserDetails();
        String token = jwtService.generateToken(userDetails);

        boolean isValid = jwtValidatorService.isTokenValid(token, userDetails);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("isTokenValid should return false when username does not match")
    void givenTokenAndDifferentUser_isTokenValid_shouldReturnFalse() {
        UserDetails userDetails = aTestUserDetails();
        UserDetails otherUser = aTestUserDetails("other@candidai.com", "ROLE_CANDIDATE");
        String token = jwtService.generateToken(userDetails);

        boolean isValid = jwtValidatorService.isTokenValid(token, otherUser);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("isTokenValid should return false and extractUsername should throw for expired token")
    void givenExpiredToken_isTokenValid_shouldReturnFalse() {
        UserDetails userDetails = aTestUserDetails();
        String expiredToken = jwtService.generateToken(userDetails, -5L, ChronoUnit.MINUTES);

        boolean isValid = jwtValidatorService.isTokenValid(expiredToken, userDetails);

        assertThat(isValid).isFalse();
        assertThatThrownBy(() -> jwtValidatorService.extractUsername(expiredToken))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    @DisplayName("isTokenValid should return false and extractUsername should throw for tampered token")
    void givenTamperedSignatureToken_isTokenValid_shouldReturnFalse() {
        UserDetails userDetails = aTestUserDetails();
        String tamperedToken = tamperedJwtService.generateToken(userDetails);

        boolean isValid = jwtValidatorService.isTokenValid(tamperedToken, userDetails);

        assertThat(isValid).isFalse();
        assertThatThrownBy(() -> jwtValidatorService.extractUsername(tamperedToken))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    @DisplayName("isTokenValid should return false for null or blank token")
    void givenNullOrBlankToken_isTokenValid_shouldReturnFalse() {
        UserDetails userDetails = aTestUserDetails();

        assertThat(jwtValidatorService.isTokenValid(null, userDetails)).isFalse();
        assertThat(jwtValidatorService.isTokenValid("   ", userDetails)).isFalse();
        assertThat(jwtValidatorService.extractUsername(null)).isNull();
    }
}
