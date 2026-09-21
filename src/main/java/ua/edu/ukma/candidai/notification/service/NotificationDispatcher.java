package ua.edu.ukma.candidai.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.notification.model.Notification;
import ua.edu.ukma.candidai.notification.model.NotificationChannel;
import ua.edu.ukma.candidai.notification.model.NotificationDeliveryStatus;
import ua.edu.ukma.candidai.notification.repository.NotificationRepository;
import ua.edu.ukma.candidai.notification.sender.NotificationSender;
import ua.edu.ukma.candidai.user.UserApi;
import ua.edu.ukma.candidai.user.UserNotificationProfile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
        Optional<UserNotificationProfile> profileOptional = userApi.getUserNotificationProfile(recipientId);
        if (profileOptional.isEmpty()) {
            log.warn("Cannot send notification: user not found with id: {}", recipientId);
            return;
        }

        UserNotificationProfile profile = profileOptional.get();
        if (profile.email() != null && !profile.email().isBlank()) {
            sendViaChannel(profile, subject, body, NotificationChannel.EMAIL);
        }
        if (profile.telegramChatId() != null && !profile.telegramChatId().isBlank()) {
            sendViaChannel(profile, subject, body, NotificationChannel.TELEGRAM);
        }
    }

    private void sendViaChannel(
            UserNotificationProfile profile,
            String subject,
            String body,
            NotificationChannel channel
    ) {
        NotificationSender sender = senders.stream()
                .filter(s -> s.getChannel() == channel)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No notification sender available for channel: " + channel
                ));

        NotificationDeliveryStatus status = NotificationDeliveryStatus.SENT;
        String errorMessage = null;

        try {
            sender.send(profile, subject, body);
        } catch (Exception e) {
            log.error("Failed to send notification via {}: {}", channel, e.getMessage());
            status = NotificationDeliveryStatus.FAILED;
            errorMessage = e.getMessage();
        }

        Instant now = generator.now();
        Notification record = Notification.builder()
                .id(generator.uuid())
                .recipientId(profile.userId())
                .recipientEmail(profile.email())
                .recipientTelegramChatId(profile.telegramChatId())
                .channel(channel)
                .subject(subject)
                .content(body)
                .status(status)
                .errorMessage(errorMessage)
                .createdAt(now)
                .sentAt(now)
                .build();

        notificationRepository.save(record);
    }
}
