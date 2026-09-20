package ua.edu.ukma.candidai.notification.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ua.edu.ukma.candidai.notification.NotificationChannel;
import ua.edu.ukma.candidai.notification.model.NotificationMessage;
import ua.edu.ukma.candidai.user.UserNotificationProfile;

@Component
@Slf4j
public class EmailNotificationSender implements NotificationSender {

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(UserNotificationProfile recipient, NotificationMessage message) {
        log.info("[EMAIL] To: {} | Subject: {} | Body: {}",
                recipient.email(), message.subject(), message.body());
    }
}
