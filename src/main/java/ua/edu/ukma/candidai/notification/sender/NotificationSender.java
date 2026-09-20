package ua.edu.ukma.candidai.notification.sender;

import ua.edu.ukma.candidai.notification.NotificationChannel;
import ua.edu.ukma.candidai.notification.model.NotificationMessage;
import ua.edu.ukma.candidai.user.UserNotificationProfile;

public interface NotificationSender {

    NotificationChannel getChannel();

    void send(UserNotificationProfile recipient, NotificationMessage message);
}
