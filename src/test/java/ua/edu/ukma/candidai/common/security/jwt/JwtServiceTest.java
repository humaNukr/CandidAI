package ua.edu.ukma.candidai.common.security.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;
import static ua.edu.ukma.candidai.common.security.SecurityTestResources.*;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(TEST_SECRET, TEST_EXPIRATION_MINUTES);

    @Test
    @DisplayName("generateToken should create valid non-blank token containing user details")
    void givenUserDetails_generateToken_shouldReturnNonBlankToken() {
        UserDetails userDetails = aTestUserDetails();

        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(TEST_EXPIRATION_MINUTES * 60L);
    }

    @Test
    @DisplayName("constructor should throw exception when secret key is shorter than 256 bits")
    void givenShortSecretKey_constructor_shouldThrowIllegalArgumentException() {
        String shortSecret = "too-short-secret-key";

        assertThatThrownBy(() -> new JwtService(shortSecret, TEST_EXPIRATION_MINUTES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("256 біт");
    }

    @Test
    @DisplayName("generateToken with past expiration should create expired token")
    void givenPastExpiration_generateToken_shouldProduceExpiredToken() {
        UserDetails userDetails = aTestUserDetails();

        String expiredToken = jwtService.generateToken(userDetails, -10L, ChronoUnit.MINUTES);

        assertThat(expiredToken).isNotBlank();
    }
}
