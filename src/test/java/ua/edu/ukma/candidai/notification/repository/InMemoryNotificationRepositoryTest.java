package ua.edu.ukma.candidai.notification.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.candidai.notification.model.Notification;
import ua.edu.ukma.candidai.notification.model.NotificationChannel;
import ua.edu.ukma.candidai.notification.model.NotificationDeliveryStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_ERROR_MESSAGE;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_ID;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_RECIPIENT_ID;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.NOTIFICATION_OR_ID_NULL_MESSAGE;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.OTHER_RECIPIENT_ID;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.SECOND_ID;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.SENT_AT;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleFailedNotification;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleNotification;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleNotificationWithNullId;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.samplePendingNotification;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleSentNotification;

class InMemoryNotificationRepositoryTest {

    private InMemoryNotificationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryNotificationRepository();
    }

    @Test
    @DisplayName("save should persist and return notification")
    void givenNotification_save_shouldPersistAndReturnNotification() {
        Notification notification = sampleNotification(
                NotificationChannel.EMAIL,
                NotificationDeliveryStatus.SENT
        );

        Notification saved = repository.save(notification);

        assertThat(saved).isEqualTo(notification);
        assertThat(repository.findById(notification.id())).contains(notification);
    }

    @Test
    @DisplayName("findById should return empty when id not found or null")
    void givenNonExistentId_findById_shouldReturnEmpty() {
        assertThat(repository.findById(DEFAULT_ID)).isEmpty();
        assertThat(repository.findById(null)).isEmpty();
    }

    @Test
    @DisplayName("findAll should return all persisted notifications")
    void givenPersistedNotifications_findAll_shouldReturnAllPersistedNotifications() {
        Notification notification1 = sampleNotification(
                NotificationChannel.EMAIL,
                NotificationDeliveryStatus.SENT
        );
        Notification notification2 = sampleNotification(
                SECOND_ID,
                DEFAULT_RECIPIENT_ID,
                NotificationChannel.TELEGRAM,
                NotificationDeliveryStatus.SENT
        );

        repository.save(notification1);
        repository.save(notification2);

        List<Notification> result = repository.findAll();

        assertThat(result).containsExactlyInAnyOrder(notification1, notification2);
    }

    @Test
    @DisplayName("findByRecipientId should filter notifications by recipient")
    void givenPersistedNotifications_findByRecipientId_shouldFilterNotificationsByRecipient() {
        Notification notification1 = sampleNotification(
                NotificationChannel.EMAIL,
                NotificationDeliveryStatus.SENT
        );
        Notification notification2 = sampleNotification(
                SECOND_ID,
                OTHER_RECIPIENT_ID,
                NotificationChannel.TELEGRAM,
                NotificationDeliveryStatus.SENT
        );

        repository.save(notification1);
        repository.save(notification2);

        List<Notification> byDefaultRecipient = repository.findByRecipientId(DEFAULT_RECIPIENT_ID);
        List<Notification> byOtherRecipient = repository.findByRecipientId(OTHER_RECIPIENT_ID);
        List<Notification> byNullRecipient = repository.findByRecipientId(null);

        assertThat(byDefaultRecipient).containsExactly(notification1);
        assertThat(byOtherRecipient).containsExactly(notification2);
        assertThat(byNullRecipient).isEmpty();
    }

    @Test
    @DisplayName("save should throw IllegalArgumentException when notification or its id is null")
    void givenNullNotificationOrNullId_save_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> repository.save(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NOTIFICATION_OR_ID_NULL_MESSAGE);

        Notification notificationWithNullId = sampleNotificationWithNullId();

        assertThatThrownBy(() -> repository.save(notificationWithNullId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NOTIFICATION_OR_ID_NULL_MESSAGE);
    }

    @Test
    @DisplayName("pending factory should create notification with PENDING status and null sentAt/errorMessage")
    void givenNotificationData_pending_shouldCreateNotificationWithPendingStatus() {
        Notification notification = samplePendingNotification();

        assertThat(notification).isEqualTo(samplePendingNotification());
    }

    @Test
    @DisplayName("markSent should transition status to SENT and preserve immutability")
    void givenPendingNotification_markSent_shouldTransitionStatusToSent() {
        Notification pending = samplePendingNotification();

        Notification sent = pending.markSent(SENT_AT);

        assertThat(sent).isNotSameAs(pending);
        assertThat(sent).isEqualTo(sampleSentNotification(SENT_AT));
    }

    @Test
    @DisplayName("markFailed should transition status to FAILED and preserve immutability")
    void givenPendingNotification_markFailed_shouldTransitionStatusToFailed() {
        Notification pending = samplePendingNotification();

        Notification failed = pending.markFailed(DEFAULT_ERROR_MESSAGE);

        assertThat(failed).isNotSameAs(pending);
        assertThat(failed).isEqualTo(sampleFailedNotification(DEFAULT_ERROR_MESSAGE));
    }
}
