package ua.edu.ukma.candidai.notification.sender;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.candidai.notification.NotificationChannel;
import ua.edu.ukma.candidai.notification.model.NotificationMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleNotificationMessage;

class TelegramNotificationSenderTest {

    private TelegramNotificationSender sender;

    @BeforeEach
    void setUp() {
        sender = new TelegramNotificationSender();
    }

    @Test
    @DisplayName("getChannel should return TELEGRAM")
    void getChannel_shouldReturnTelegram() {
        assertThat(sender.getChannel()).isEqualTo(NotificationChannel.TELEGRAM);
    }

    @Test
    @DisplayName("send should execute without error")
    void send_shouldExecuteWithoutError() {
        NotificationMessage message = sampleNotificationMessage();
        assertThatCode(() -> sender.send(message)).doesNotThrowAnyException();
    }
}
