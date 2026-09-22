package ua.edu.ukma.candidai.notification.sender;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.edu.ukma.candidai.notification.model.NotificationChannel;
import ua.edu.ukma.candidai.notification.telegram.TelegramBotClient;
import ua.edu.ukma.candidai.user.UserNotificationProfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_TELEGRAM_CHAT_ID;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.HTML_ESCAPED_BODY;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.HTML_ESCAPED_FORMATTED_TEXT;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.HTML_ESCAPED_SUBJECT;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.PARSE_MODE_HTML;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleUserNotificationProfile;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.userProfileWithBlankTelegramChatId;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.userProfileWithNullTelegramChatId;

@ExtendWith(MockitoExtension.class)
class TelegramNotificationSenderTest {

    @Mock
    private TelegramBotClient botClient;

    private TelegramNotificationSender sender;

    @BeforeEach
    void setUp() {
        sender = new TelegramNotificationSender(botClient);
    }

    @Test
    @DisplayName("getChannel should return TELEGRAM")
    void givenSender_getChannel_shouldReturnTelegram() {
        assertThat(sender.getChannel()).isEqualTo(NotificationChannel.TELEGRAM);
    }

    @Test
    @DisplayName("send should format and HTML-escape text and delegate to bot client")
    void givenRecipientAndContent_send_shouldFormatHtmlAndDelegateToBotClient() {
        UserNotificationProfile recipient = sampleUserNotificationProfile();

        sender.send(recipient, HTML_ESCAPED_SUBJECT, HTML_ESCAPED_BODY);

        verify(botClient).sendMessage(DEFAULT_TELEGRAM_CHAT_ID, HTML_ESCAPED_FORMATTED_TEXT, PARSE_MODE_HTML);
    }

    @Test
    @DisplayName("supports should return true when recipient has non-blank telegramChatId")
    void givenRecipientWithTelegramChatId_supports_shouldReturnTrue() {
        assertThat(sender.supports(sampleUserNotificationProfile())).isTrue();
    }

    @Test
    @DisplayName("supports should return false when recipient is null")
    void givenNullRecipient_supports_shouldReturnFalse() {
        assertThat(sender.supports(null)).isFalse();
    }

    @Test
    @DisplayName("supports should return false when recipient telegramChatId is null")
    void givenRecipientWithNullTelegramChatId_supports_shouldReturnFalse() {
        assertThat(sender.supports(userProfileWithNullTelegramChatId())).isFalse();
    }

    @Test
    @DisplayName("supports should return false when recipient telegramChatId is blank")
    void givenRecipientWithBlankTelegramChatId_supports_shouldReturnFalse() {
        assertThat(sender.supports(userProfileWithBlankTelegramChatId())).isFalse();
    }
}
