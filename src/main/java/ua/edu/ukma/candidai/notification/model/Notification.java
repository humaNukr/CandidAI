package ua.edu.ukma.candidai.notification.model;

import java.time.Instant;
import java.util.UUID;

public record Notification(
        UUID id,
        UUID recipientId,
        String recipientEmail,
        String recipientTelegramChatId,
        NotificationChannel channel,
        String subject,
        String content,
        NotificationDeliveryStatus status,
        String errorMessage,
        Instant createdAt,
        Instant sentAt
) {

    public static Notification pending(
            UUID id,
            UUID recipientId,
            String recipientEmail,
            String recipientTelegramChatId,
            NotificationChannel channel,
            String subject,
            String content,
            Instant createdAt
    ) {
        return new Notification(
                id,
                recipientId,
                recipientEmail,
                recipientTelegramChatId,
                channel,
                subject,
                content,
                NotificationDeliveryStatus.PENDING,
                null,
                createdAt,
                null
        );
    }

    public Notification markSent(Instant sentAt) {
        return new Notification(
                id,
                recipientId,
                recipientEmail,
                recipientTelegramChatId,
                channel,
                subject,
                content,
                NotificationDeliveryStatus.SENT,
                null,
                createdAt,
                sentAt
        );
    }

    public Notification markFailed(String errorMessage) {
        return new Notification(
                id,
                recipientId,
                recipientEmail,
                recipientTelegramChatId,
                channel,
                subject,
                content,
                NotificationDeliveryStatus.FAILED,
                errorMessage,
                createdAt,
                null
        );
    }
}
