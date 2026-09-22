package ua.edu.ukma.candidai.notification.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_EMAIL;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_HTML_CONTENT;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_SUBJECT;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.expectedEmailSendErrorMessage;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleMessagingException;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleMimeMessage;

@ExtendWith(MockitoExtension.class)
class SmtpEmailTransportTest {

    @Mock
    private JavaMailSender mailSender;

    private SmtpEmailTransport transport;

    @BeforeEach
    void setUp() {
        transport = new SmtpEmailTransport(mailSender);
    }

    @Test
    @DisplayName("sendEmail should construct and send MimeMessage successfully")
    void givenRecipientAndContent_sendEmail_shouldConstructAndSendMimeMessage() {
        MimeMessage mimeMessage = sampleMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        transport.sendEmail(DEFAULT_EMAIL, DEFAULT_SUBJECT, DEFAULT_HTML_CONTENT);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendEmail should wrap MessagingException into MailSendException")
    void givenMessagingException_sendEmail_shouldWrapIntoMailSendException() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(sampleMessagingException())
                .when(mimeMessage).setSubject(DEFAULT_SUBJECT, StandardCharsets.UTF_8.name());

        assertThatThrownBy(() -> transport.sendEmail(DEFAULT_EMAIL, DEFAULT_SUBJECT, DEFAULT_HTML_CONTENT))
                .isInstanceOf(MailSendException.class)
                .hasMessageContaining(expectedEmailSendErrorMessage(DEFAULT_EMAIL))
                .hasCauseInstanceOf(MessagingException.class);
    }
}
