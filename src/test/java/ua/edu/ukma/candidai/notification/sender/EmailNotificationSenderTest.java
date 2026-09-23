package ua.edu.ukma.candidai.notification.sender;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.edu.ukma.candidai.notification.email.EmailTemplateRenderer;
import ua.edu.ukma.candidai.notification.email.EmailTransport;
import ua.edu.ukma.candidai.notification.model.NotificationChannel;
import ua.edu.ukma.candidai.user.UserNotificationProfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_BODY;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_EMAIL;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_HTML_CONTENT;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_SUBJECT;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleUserNotificationProfile;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.userProfileWithBlankEmail;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.userProfileWithNullEmail;

@ExtendWith(MockitoExtension.class)
class EmailNotificationSenderTest {

    @Mock
    private EmailTemplateRenderer templateRenderer;

    @Mock
    private EmailTransport emailTransport;

    private EmailNotificationSender sender;

    @BeforeEach
    void setUp() {
        sender = new EmailNotificationSender(templateRenderer, emailTransport);
    }

    @Test
    @DisplayName("getChannel should return EMAIL")
    void givenSender_getChannel_shouldReturnEmail() {
        assertThat(sender.getChannel()).isEqualTo(NotificationChannel.EMAIL);
    }

    @Test
    @DisplayName("supports should return true when recipient has non-blank email")
    void givenRecipientWithEmail_supports_shouldReturnTrue() {
        assertThat(sender.supports(sampleUserNotificationProfile())).isTrue();
    }

    @Test
    @DisplayName("supports should return false when recipient is null")
    void givenNullRecipient_supports_shouldReturnFalse() {
        assertThat(sender.supports(null)).isFalse();
    }

    @Test
    @DisplayName("supports should return false when recipient email is null")
    void givenRecipientWithoutEmail_supports_shouldReturnFalse() {
        assertThat(sender.supports(userProfileWithNullEmail())).isFalse();
    }

    @Test
    @DisplayName("supports should return false when recipient email is blank")
    void givenRecipientWithBlankEmail_supports_shouldReturnFalse() {
        assertThat(sender.supports(userProfileWithBlankEmail())).isFalse();
    }

    @Test
    @DisplayName("send should render template and delegate to email transport")
    void givenRecipientAndContent_send_shouldRenderHtmlAndDelegateToEmailTransport() {
        UserNotificationProfile recipient = sampleUserNotificationProfile();
        when(templateRenderer.render(recipient.fullName(), DEFAULT_SUBJECT, DEFAULT_BODY))
                .thenReturn(DEFAULT_HTML_CONTENT);

        sender.send(recipient, DEFAULT_SUBJECT, DEFAULT_BODY);

        verify(emailTransport).sendEmail(DEFAULT_EMAIL, DEFAULT_SUBJECT, DEFAULT_HTML_CONTENT);
    }
}
