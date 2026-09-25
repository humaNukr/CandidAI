package ua.edu.ukma.candidai.notification.sender;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ua.edu.ukma.candidai.notification.email.EmailTemplateRenderer;
import ua.edu.ukma.candidai.notification.email.EmailTransport;
import ua.edu.ukma.candidai.notification.model.NotificationChannel;
import ua.edu.ukma.candidai.user.UserNotificationProfile;

@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private final EmailTemplateRenderer templateRenderer;
    private final EmailTransport emailTransport;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public boolean supports(UserNotificationProfile recipient) {
        return recipient != null && recipient.email() != null && !recipient.email().isBlank();
    }

    @Override
    public void send(UserNotificationProfile recipient, String subject, String body) {
        String html = templateRenderer.render(recipient.fullName(), subject, body);
        emailTransport.sendEmail(recipient.email(), subject, html);
    }
}
