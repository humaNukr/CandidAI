package ua.edu.ukma.candidai.user.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static ua.edu.ukma.candidai.user.security.UserSecurityTestResources.*;

class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder = new PasswordEncoderConfig().passwordEncoder();

    @Test
    void givenRawPassword_encode_shouldProduceArgon2PrefixedHash() {
        String hash = passwordEncoder.encode(RAW_PASSWORD);

        assertThat(hash)
                .isNotBlank()
                .startsWith("{argon2}$argon2id$");
    }

    @Test
    void givenMatchingPasswordAndHash_matches_shouldReturnTrue() {
        String hash = passwordEncoder.encode(RAW_PASSWORD);

        boolean result = passwordEncoder.matches(RAW_PASSWORD, hash);

        assertThat(result).isTrue();
    }

    @Test
    void givenMismatchedPasswordAndHash_matches_shouldReturnFalse() {
        String hash = passwordEncoder.encode(RAW_PASSWORD);

        boolean result = passwordEncoder.matches(WRONG_PASSWORD, hash);

        assertThat(result).isFalse();
    }
}
