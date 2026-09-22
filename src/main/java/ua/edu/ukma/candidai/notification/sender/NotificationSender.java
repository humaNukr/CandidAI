package ua.edu.ukma.candidai.notification.sender;

import ua.edu.ukma.candidai.notification.model.NotificationChannel;
import ua.edu.ukma.candidai.user.UserNotificationProfile;

public interface NotificationSender {

    NotificationChannel getChannel();

    boolean supports(UserNotificationProfile recipient);

    void send(UserNotificationProfile recipient, String subject, String body);
}
