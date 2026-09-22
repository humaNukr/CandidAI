package ua.edu.ukma.candidai.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.notification.NotificationChannel;
import ua.edu.ukma.candidai.notification.NotificationDeliveryStatus;
import ua.edu.ukma.candidai.notification.model.Notification;
import ua.edu.ukma.candidai.notification.model.NotificationMessage;
import ua.edu.ukma.candidai.notification.repository.NotificationRepository;
import ua.edu.ukma.candidai.notification.sender.NotificationSender;

import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_ID;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_NOW;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.SECOND_ID;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.emailOnlyNotificationMessage;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleNotificationBuilder;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleNotificationMessage;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.telegramOnlyNotificationMessage;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private NotificationSender emailSender;

    @Mock
    private NotificationSender telegramSender;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private CommonGenerator generator;

    private NotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new NotificationDispatcher(
                List.of(emailSender, telegramSender),
                notificationRepository,
                generator
        );
    }

    @Test
    @DisplayName("dispatches email when email present")
    void givenEmailPresent_dispatch_shouldSendEmailAndPersistSentNotification() {
        NotificationMessage message = emailOnlyNotificationMessage();
        Notification expectedNotification = sampleNotificationBuilder()
                .channel(NotificationChannel.EMAIL)
                .recipientTelegramChatId(null)
                .status(NotificationDeliveryStatus.SENT)
                .build();

        when(emailSender.getChannel()).thenReturn(NotificationChannel.EMAIL);
        doNothing().when(emailSender).send(message);
        when(generator.now()).thenReturn(DEFAULT_NOW);
        when(generator.uuid()).thenReturn(DEFAULT_ID);

        dispatcher.dispatch(message);

        verify(emailSender).send(message);
        verify(notificationRepository).save(expectedNotification);
        verify(telegramSender, never()).send(message);
    }

    @Test
    @DisplayName("dispatches telegram when telegram present")
    void givenTelegramPresent_dispatch_shouldSendTelegramAndPersistSentNotification() {
        NotificationMessage message = telegramOnlyNotificationMessage();
        Notification expectedNotification = sampleNotificationBuilder()
                .channel(NotificationChannel.TELEGRAM)
                .recipientEmail(null)
                .status(NotificationDeliveryStatus.SENT)
                .build();

        when(emailSender.getChannel()).thenReturn(NotificationChannel.EMAIL);
        when(telegramSender.getChannel()).thenReturn(NotificationChannel.TELEGRAM);
        doNothing().when(telegramSender).send(message);
        when(generator.now()).thenReturn(DEFAULT_NOW);
        when(generator.uuid()).thenReturn(DEFAULT_ID);

        dispatcher.dispatch(message);

        verify(telegramSender).send(message);
        verify(notificationRepository).save(expectedNotification);
        verify(emailSender, never()).send(message);
    }

    @Test
    @DisplayName("dispatches both when both present")
    void givenBothPresent_dispatch_shouldSendBothAndPersistNotificationsWithDistinctIds() {
        NotificationMessage message = sampleNotificationMessage();
        Notification expectedEmail = sampleNotificationBuilder()
                .id(DEFAULT_ID)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationDeliveryStatus.SENT)
                .build();
        Notification expectedTelegram = sampleNotificationBuilder()
                .id(SECOND_ID)
                .channel(NotificationChannel.TELEGRAM)
                .status(NotificationDeliveryStatus.SENT)
                .build();

        when(emailSender.getChannel()).thenReturn(NotificationChannel.EMAIL);
        when(telegramSender.getChannel()).thenReturn(NotificationChannel.TELEGRAM);
        doNothing().when(emailSender).send(message);
        doNothing().when(telegramSender).send(message);
        when(generator.now()).thenReturn(DEFAULT_NOW);
        when(generator.uuid()).thenReturn(DEFAULT_ID, SECOND_ID);

        dispatcher.dispatch(message);

        verify(emailSender).send(message);
        verify(telegramSender).send(message);
        verify(notificationRepository).save(expectedEmail);
        verify(notificationRepository).save(expectedTelegram);
    }

    @Test
    @DisplayName("records FAILED when sender throws exception")
    void givenSenderThrowsException_dispatch_shouldRecordFailedStatus() {
        NotificationMessage message = emailOnlyNotificationMessage();
        Notification expectedNotification = sampleNotificationBuilder()
                .channel(NotificationChannel.EMAIL)
                .recipientTelegramChatId(null)
                .status(NotificationDeliveryStatus.FAILED)
                .errorMessage("SMTP connection timeout")
                .build();

        when(emailSender.getChannel()).thenReturn(NotificationChannel.EMAIL);
        doThrow(new RuntimeException("SMTP connection timeout")).when(emailSender).send(message);
        when(generator.now()).thenReturn(DEFAULT_NOW);
        when(generator.uuid()).thenReturn(DEFAULT_ID);

        dispatcher.dispatch(message);

        verify(emailSender).send(message);
        verify(notificationRepository).save(expectedNotification);
        verify(telegramSender, never()).send(message);
    }
}
