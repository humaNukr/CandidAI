package ua.edu.ukma.candidai.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class MaskingMessageConverterTest {

    private final MaskingMessageConverter converter = new MaskingMessageConverter();

    @Test
    @DisplayName("mask - should redact password in plain text")
    void givenMessageWithPassword_mask_shouldRedactPassword() {
        String input = "User login attempt: password: mySecretPassword123, email: candidate@candidai.ua";
        String expected = "User login attempt: password: ***, email: candidate@candidai.ua";

        String actual = converter.mask(input);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("mask - should redact password and passwordHash in JSON format")
    void givenJsonMessageWithSensitiveFields_mask_shouldRedactPasswordAndHash() {
        String input = "{\"password\": \"plainPassword12\", \"passwordHash\": \"$2a$10$abcdef\", \"role\": \"ADMIN\"}";
        String expected = "{\"password\": \"***\", \"passwordHash\": \"***\", \"role\": \"ADMIN\"}";

        String actual = converter.mask(input);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("mask - should redact multi-word passphrases with spaces in JSON")
    void givenJsonWithMultiWordPassphrase_mask_shouldRedactEntirePassphrase() {
        String input = "{\"password\": \"secret pass 2026\", \"secret\": \"correct horse battery staple\"}";
        String expected = "{\"password\": \"***\", \"secret\": \"***\"}";

        String actual = converter.mask(input);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("mask - should redact multi-word passphrase with escaped quotes and special characters")
    void givenPassphraseWithEscapedQuotes_mask_shouldRedactProperly() {
        String input = "{\"password\": \"p@$$w0rd with \\\"quotes\\\" and spaces!#$\", \"role\": \"USER\"}";
        String expected = "{\"password\": \"***\", \"role\": \"USER\"}";

        String actual = converter.mask(input);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("mask - should redact single-quoted passphrase with spaces")
    void givenSingleQuotedPassphrase_mask_shouldRedactEntirePassphrase() {
        String input = "'secret': 'multi-word single quoted passphrase', 'status': 'OK'";
        String expected = "'secret': '***', 'status': 'OK'";

        String actual = converter.mask(input);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("mask - should redact empty password in JSON")
    void givenEmptyPasswordInJson_mask_shouldRedactGracefully() {
        String input = "{\"password\": \"\", \"username\": \"admin\"}";
        String expected = "{\"password\": \"***\", \"username\": \"admin\"}";

        String actual = converter.mask(input);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("mask - should redact apiKey, token, and secret")
    void givenMessageWithSecretAndToken_mask_shouldRedactBoth() {
        String input = "Credentials: apiKey: secret-api-key-999, token: eyJhbGciOiJIUzI1NiJ9, secret: superSecret";
        String expected = "Credentials: apiKey: ***, token: ***, secret: ***";

        String actual = converter.mask(input);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("mask - should redact Bearer token from authorization headers")
    void givenMessageWithBearerToken_mask_shouldRedactBearerToken() {
        String input = "Header Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.payload.sig";
        String expected = "Header Authorization: Bearer ***";

        String actual = converter.mask(input);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("mask - should mask middle digits of candidate phone number")
    void givenMessageWithCandidatePhoneNumber_mask_shouldMaskMiddleDigits() {
        String input = "Candidate contact: +380971231234 and local: 0971234567";
        String expected = "Candidate contact: +380****1234 and local: 097****4567";

        String actual = converter.mask(input);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("convert - should mask formatted message from ILoggingEvent")
    void givenLoggingEvent_convert_shouldReturnMaskedMessage() {
        ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
        when(event.getFormattedMessage()).thenReturn("User updated password=superSecretPassword42");

        String actual = converter.convert(event);

        assertThat(actual).isEqualTo("User updated password=***");
    }

    @Test
    @DisplayName("transform - should mask input string directly")
    void givenLoggingEventAndInput_transform_shouldReturnMaskedMessage() {
        ILoggingEvent event = Mockito.mock(ILoggingEvent.class);

        String actual = converter.transform(event, "Authorization: Bearer token12345");

        assertThat(actual).isEqualTo("Authorization: Bearer ***");
    }

    @Test
    @DisplayName("mask - should return null or empty when given null or empty input")
    void givenNullOrEmptyMessage_mask_shouldHandleGracefully() {
        assertThat(converter.mask(null)).isNull();
        assertThat(converter.mask("")).isEmpty();
    }

    @Test
    @DisplayName("mask - should leave message unchanged when no sensitive data is present")
    void givenCleanMessageWithoutSensitiveData_mask_shouldReturnOriginalMessage() {
        String message = "Application 550e8400-e29b-41d4-a716-446655440000 transitioned to INTERVIEW";

        assertThat(converter.mask(message)).isEqualTo(message);
    }
}
