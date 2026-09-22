package ua.edu.ukma.candidai.notification;

import ua.edu.ukma.candidai.notification.model.Notification;
import ua.edu.ukma.candidai.notification.model.NotificationMessage;
import ua.edu.ukma.candidai.user.UserNotificationProfile;
import ua.edu.ukma.candidai.vacancy.VacancyStatusChangedEvent;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;

import java.time.Instant;
import java.util.UUID;

public final class NotificationTestResources {

    public static final UUID DEFAULT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID SECOND_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    public static final UUID DEFAULT_RECIPIENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID OTHER_RECIPIENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000088");
    public static final UUID DEFAULT_VACANCY_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    public static final String DEFAULT_VACANCY_TITLE = "Senior Java Engineer";
    public static final String DEFAULT_EMAIL = "candidate@example.com";
    public static final String DEFAULT_TELEGRAM_CHAT_ID = "123456789";
    public static final Instant DEFAULT_NOW = Instant.parse("2026-09-20T10:00:00Z");
    public static final String DEFAULT_SUBJECT = "Статус вашої вакансії змінено";
    public static final String DEFAULT_BODY = "Вакансія 'Senior Java Engineer' змінила статус на CLOSED";

    private NotificationTestResources() {
    }

    public static NotificationMessage sampleNotificationMessage() {
        return NotificationMessage.builder()
                .recipientId(DEFAULT_RECIPIENT_ID)
                .recipientEmail(DEFAULT_EMAIL)
                .recipientTelegramChatId(DEFAULT_TELEGRAM_CHAT_ID)
                .subject(DEFAULT_SUBJECT)
                .body(DEFAULT_BODY)
                .build();
    }

    public static NotificationMessage emailOnlyNotificationMessage() {
        return NotificationMessage.builder()
                .recipientId(DEFAULT_RECIPIENT_ID)
                .recipientEmail(DEFAULT_EMAIL)
                .recipientTelegramChatId(null)
                .subject(DEFAULT_SUBJECT)
                .body(DEFAULT_BODY)
                .build();
    }

    public static NotificationMessage telegramOnlyNotificationMessage() {
        return NotificationMessage.builder()
                .recipientId(DEFAULT_RECIPIENT_ID)
                .recipientEmail(null)
                .recipientTelegramChatId(DEFAULT_TELEGRAM_CHAT_ID)
                .subject(DEFAULT_SUBJECT)
                .body(DEFAULT_BODY)
                .build();
    }

    public static VacancyStatusChangedEvent sampleVacancyStatusChangedEvent() {
        return new VacancyStatusChangedEvent(
                DEFAULT_VACANCY_ID,
                DEFAULT_VACANCY_TITLE,
                DEFAULT_RECIPIENT_ID,
                VacancyStatus.OPEN,
                VacancyStatus.CLOSED,
                DEFAULT_NOW
        );
    }

    public static UserNotificationProfile sampleUserNotificationProfile() {
        return new UserNotificationProfile(
                DEFAULT_RECIPIENT_ID,
                "John Doe",
                DEFAULT_EMAIL,
                DEFAULT_TELEGRAM_CHAT_ID
        );
    }

    public static Notification.NotificationBuilder sampleNotificationBuilder() {
        return Notification.builder()
                .id(DEFAULT_ID)
                .recipientId(DEFAULT_RECIPIENT_ID)
                .recipientEmail(DEFAULT_EMAIL)
                .recipientTelegramChatId(DEFAULT_TELEGRAM_CHAT_ID)
                .channel(NotificationChannel.EMAIL)
                .subject(DEFAULT_SUBJECT)
                .content(DEFAULT_BODY)
                .status(NotificationDeliveryStatus.SENT)
                .errorMessage(null)
                .createdAt(DEFAULT_NOW)
                .sentAt(DEFAULT_NOW);
    }

    public static Notification sampleNotification(NotificationChannel channel, NotificationDeliveryStatus status) {
        return sampleNotification(channel, status, null);
    }

    public static Notification sampleNotification(
            NotificationChannel channel,
            NotificationDeliveryStatus status,
            String errorMessage
    ) {
        return sampleNotificationBuilder()
                .channel(channel)
                .status(status)
                .errorMessage(errorMessage)
                .build();
    }
}
