package ua.edu.ukma.candidai.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.notification.model.Notification;
import ua.edu.ukma.candidai.notification.repository.NotificationRepository;
import ua.edu.ukma.candidai.notification.sender.NotificationSender;
import ua.edu.ukma.candidai.user.UserApi;
import ua.edu.ukma.candidai.user.UserNotificationProfile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcher {

    private final UserApi userApi;
    private final List<NotificationSender> senders;
    private final NotificationRepository notificationRepository;
    private final CommonGenerator generator;

    public void dispatch(UUID recipientId, String subject, String body) {
        userApi.getUserNotificationProfile(recipientId).ifPresentOrElse(
                profile -> dispatchToProfile(profile, subject, body),
                () -> log.warn("Cannot send notification: user profile not found for id: {}", recipientId)
        );
    }

    private void dispatchToProfile(UserNotificationProfile profile, String subject, String body) {
        for (NotificationSender sender : senders) {
            if (sender.supports(profile)) {
                sendAndRecord(sender, profile, subject, body);
            }
        }
    }

    private void sendAndRecord(
            NotificationSender sender,
            UserNotificationProfile profile,
            String subject,
            String body
    ) {
        Instant now = generator.now();
        Notification notification = Notification.pending(
                generator.uuid(),
                profile.userId(),
                profile.email(),
                profile.telegramChatId(),
                sender.getChannel(),
                subject,
                body,
                now
        );

        try {
            sender.send(profile, subject, body);
            Notification sentRecord = notification.markSent(generator.now());
            notificationRepository.save(sentRecord);
            log.info("[NOTIFICATION] Sent {} to recipient {}", sender.getChannel(), profile.userId());
        } catch (Exception e) {
            log.error("Failed to send notification via {}: {}", sender.getChannel(), e.getMessage());
            Notification failedRecord = notification.markFailed(e.getMessage());
            notificationRepository.save(failedRecord);
        }
    }
}
