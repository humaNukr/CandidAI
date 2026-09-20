package ua.edu.ukma.candidai.notification.sender;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.candidai.notification.NotificationChannel;
import ua.edu.ukma.candidai.notification.model.NotificationMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleNotificationMessage;

class EmailNotificationSenderTest {

    private EmailNotificationSender sender;

    @BeforeEach
    void setUp() {
        sender = new EmailNotificationSender();
    }

    @Test
    @DisplayName("getChannel should return EMAIL")
    void getChannel_shouldReturnEmail() {
        assertThat(sender.getChannel()).isEqualTo(NotificationChannel.EMAIL);
    }

    @Test
    @DisplayName("send should execute without error")
    void send_shouldExecuteWithoutError() {
        NotificationMessage message = sampleNotificationMessage();
        assertThatCode(() -> sender.send(message)).doesNotThrowAnyException();
    }
}
