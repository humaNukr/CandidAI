package ua.edu.ukma.candidai.notification.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.candidai.notification.NotificationChannel;
import ua.edu.ukma.candidai.notification.NotificationDeliveryStatus;
import ua.edu.ukma.candidai.notification.model.Notification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_ID;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_RECIPIENT_ID;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.OTHER_RECIPIENT_ID;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.SECOND_ID;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleNotification;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleNotificationBuilder;

class InMemoryNotificationRepositoryTest {

    private InMemoryNotificationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryNotificationRepository();
    }

    @Test
    void shouldSaveAndFindById() {
        Notification notification = sampleNotification(
                NotificationChannel.EMAIL,
                NotificationDeliveryStatus.SENT
        );

        Notification saved = repository.save(notification);

        assertThat(saved).isEqualTo(notification);
        assertThat(repository.findById(notification.getId())).contains(notification);
    }

    @Test
    void shouldReturnEmptyWhenNotFoundOrIdIsNull() {
        assertThat(repository.findById(DEFAULT_ID)).isEmpty();
        assertThat(repository.findById(null)).isEmpty();
    }

    @Test
    void shouldFindAllNotifications() {
        Notification notification1 = sampleNotification(
                NotificationChannel.EMAIL,
                NotificationDeliveryStatus.SENT
        );
        Notification notification2 = sampleNotificationBuilder()
                .id(SECOND_ID)
                .channel(NotificationChannel.TELEGRAM)
                .build();

        repository.save(notification1);
        repository.save(notification2);

        List<Notification> result = repository.findAll();

        assertThat(result).containsExactlyInAnyOrder(notification1, notification2);
    }

    @Test
    void shouldFindByRecipientId() {
        Notification notification1 = sampleNotification(
                NotificationChannel.EMAIL,
                NotificationDeliveryStatus.SENT
        );
        Notification notification2 = sampleNotificationBuilder()
                .id(SECOND_ID)
                .recipientId(OTHER_RECIPIENT_ID)
                .channel(NotificationChannel.TELEGRAM)
                .build();

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
    void shouldThrowIllegalArgumentExceptionWhenSavingNullOrNullId() {
        assertThatThrownBy(() -> repository.save(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Notification and its id must not be null");

        Notification notificationWithNullId = sampleNotificationBuilder()
                .id(null)
                .build();

        assertThatThrownBy(() -> repository.save(notificationWithNullId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Notification and its id must not be null");
    }
}
