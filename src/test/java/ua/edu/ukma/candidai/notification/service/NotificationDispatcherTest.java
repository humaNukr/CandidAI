package ua.edu.ukma.candidai.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.notification.model.Notification;
import ua.edu.ukma.candidai.notification.model.NotificationChannel;
import ua.edu.ukma.candidai.notification.model.NotificationDeliveryStatus;
import ua.edu.ukma.candidai.notification.repository.NotificationRepository;
import ua.edu.ukma.candidai.notification.sender.NotificationSender;
import ua.edu.ukma.candidai.user.UserApi;
import ua.edu.ukma.candidai.user.UserNotificationProfile;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_BODY;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_EMAIL;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_ERROR_MESSAGE;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_ID;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_NOW;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_RECIPIENT_ID;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_SUBJECT;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.SECOND_ID;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleNotification;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleSmtpException;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleUserNotificationProfile;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private NotificationSender emailSender;

    @Mock
    private NotificationSender telegramSender;

    @Mock
    private UserApi userApi;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private CommonGenerator generator;

    private NotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new NotificationDispatcher(
                userApi,
                List.of(emailSender, telegramSender),
                notificationRepository,
                generator
        );
    }

    @Test
    @DisplayName("does nothing when user notification profile not found")
    void givenUserNotFound_dispatch_shouldDoNothing() {
        when(userApi.getUserNotificationProfile(DEFAULT_RECIPIENT_ID)).thenReturn(Optional.empty());

        dispatcher.dispatch(DEFAULT_RECIPIENT_ID, DEFAULT_SUBJECT, DEFAULT_BODY);

        verifyNoInteractions(emailSender, telegramSender, notificationRepository, generator);
    }

    @Test
    @DisplayName("does not send or record when sender does not support profile")
    void givenSenderDoesNotSupportProfile_dispatch_shouldNotSendOrPersistNotification() {
        UserNotificationProfile profile = sampleUserNotificationProfile();

        when(userApi.getUserNotificationProfile(DEFAULT_RECIPIENT_ID)).thenReturn(Optional.of(profile));
        when(emailSender.supports(profile)).thenReturn(false);
        when(telegramSender.supports(profile)).thenReturn(false);

        dispatcher.dispatch(DEFAULT_RECIPIENT_ID, DEFAULT_SUBJECT, DEFAULT_BODY);

        verify(emailSender, never()).send(profile, DEFAULT_SUBJECT, DEFAULT_BODY);
        verify(telegramSender, never()).send(profile, DEFAULT_SUBJECT, DEFAULT_BODY);
        verifyNoInteractions(notificationRepository, generator);
    }

    @Test
    @DisplayName("sends and saves notification when sender supports profile")
    void givenSenderSupportsProfile_dispatch_shouldSendAndPersistNotification() {
        UserNotificationProfile profile = sampleUserNotificationProfile();
        Notification expectedNotification = sampleNotification(
                profile,
                NotificationChannel.EMAIL,
                NotificationDeliveryStatus.SENT
        );

        when(userApi.getUserNotificationProfile(DEFAULT_RECIPIENT_ID)).thenReturn(Optional.of(profile));
        when(emailSender.supports(profile)).thenReturn(true);
        when(emailSender.getChannel()).thenReturn(NotificationChannel.EMAIL);
        when(telegramSender.supports(profile)).thenReturn(false);
        doNothing().when(emailSender).send(profile, DEFAULT_SUBJECT, DEFAULT_BODY);
        when(generator.now()).thenReturn(DEFAULT_NOW);
        when(generator.uuid()).thenReturn(DEFAULT_ID);

        dispatcher.dispatch(DEFAULT_RECIPIENT_ID, DEFAULT_SUBJECT, DEFAULT_BODY);

        verify(emailSender).send(profile, DEFAULT_SUBJECT, DEFAULT_BODY);
        verify(notificationRepository).save(expectedNotification);
        verify(telegramSender, never()).send(profile, DEFAULT_SUBJECT, DEFAULT_BODY);
    }

    @Test
    @DisplayName("records FAILED when sender throws exception and continues to remaining senders")
    void givenSenderThrowsException_dispatch_shouldRecordFailedStatusAndContinueToRemainingSenders() {
        UserNotificationProfile profile = sampleUserNotificationProfile();
        RuntimeException exception = sampleSmtpException();
        Notification expectedEmail = sampleNotification(
                DEFAULT_ID,
                profile,
                NotificationChannel.EMAIL,
                NotificationDeliveryStatus.FAILED,
                DEFAULT_ERROR_MESSAGE
        );
        Notification expectedTelegram = sampleNotification(
                SECOND_ID,
                profile,
                NotificationChannel.TELEGRAM,
                NotificationDeliveryStatus.SENT
        );

        when(userApi.getUserNotificationProfile(DEFAULT_RECIPIENT_ID)).thenReturn(Optional.of(profile));
        when(emailSender.supports(profile)).thenReturn(true);
        when(emailSender.getChannel()).thenReturn(NotificationChannel.EMAIL);
        doThrow(exception).when(emailSender).send(profile, DEFAULT_SUBJECT, DEFAULT_BODY);

        when(telegramSender.supports(profile)).thenReturn(true);
        when(telegramSender.getChannel()).thenReturn(NotificationChannel.TELEGRAM);
        doNothing().when(telegramSender).send(profile, DEFAULT_SUBJECT, DEFAULT_BODY);

        when(generator.now()).thenReturn(DEFAULT_NOW);
        when(generator.uuid()).thenReturn(DEFAULT_ID, SECOND_ID);

        dispatcher.dispatch(DEFAULT_RECIPIENT_ID, DEFAULT_SUBJECT, DEFAULT_BODY);

        verify(emailSender).send(profile, DEFAULT_SUBJECT, DEFAULT_BODY);
        verify(telegramSender).send(profile, DEFAULT_SUBJECT, DEFAULT_BODY);
        verify(notificationRepository).save(expectedEmail);
        verify(notificationRepository).save(expectedTelegram);
    }

    @Test
    @DisplayName("dispatches to all supporting senders with distinct notification ids")
    void givenMultipleSendersSupportProfile_dispatch_shouldUseDistinctIdsAndPersistBoth() {
        UserNotificationProfile profile = sampleUserNotificationProfile();
        Notification expectedEmail = sampleNotification(
                DEFAULT_ID,
                profile,
                NotificationChannel.EMAIL,
                NotificationDeliveryStatus.SENT
        );
        Notification expectedTelegram = sampleNotification(
                SECOND_ID,
                profile,
                NotificationChannel.TELEGRAM,
                NotificationDeliveryStatus.SENT
        );

        when(userApi.getUserNotificationProfile(DEFAULT_RECIPIENT_ID)).thenReturn(Optional.of(profile));
        when(emailSender.supports(profile)).thenReturn(true);
        when(emailSender.getChannel()).thenReturn(NotificationChannel.EMAIL);
        when(telegramSender.supports(profile)).thenReturn(true);
        when(telegramSender.getChannel()).thenReturn(NotificationChannel.TELEGRAM);
        doNothing().when(emailSender).send(profile, DEFAULT_SUBJECT, DEFAULT_BODY);
        doNothing().when(telegramSender).send(profile, DEFAULT_SUBJECT, DEFAULT_BODY);
        when(generator.now()).thenReturn(DEFAULT_NOW);
        when(generator.uuid()).thenReturn(DEFAULT_ID, SECOND_ID);

        dispatcher.dispatch(DEFAULT_RECIPIENT_ID, DEFAULT_SUBJECT, DEFAULT_BODY);

        verify(emailSender).send(profile, DEFAULT_SUBJECT, DEFAULT_BODY);
        verify(telegramSender).send(profile, DEFAULT_SUBJECT, DEFAULT_BODY);
        verify(notificationRepository).save(expectedEmail);
        verify(notificationRepository).save(expectedTelegram);
    }

    @Test
    @DisplayName("sends and saves notification when user profile found by email and sender supports profile")
    void givenUserFoundByEmail_dispatchByEmail_shouldSendAndPersistNotification() {
        UserNotificationProfile profile = sampleUserNotificationProfile();
        Notification expectedNotification = sampleNotification(
                profile,
                NotificationChannel.EMAIL,
                NotificationDeliveryStatus.SENT
        );

        when(userApi.getUserNotificationProfileByEmail(DEFAULT_EMAIL)).thenReturn(Optional.of(profile));
        when(emailSender.supports(profile)).thenReturn(true);
        when(emailSender.getChannel()).thenReturn(NotificationChannel.EMAIL);
        when(telegramSender.supports(profile)).thenReturn(false);
        doNothing().when(emailSender).send(profile, DEFAULT_SUBJECT, DEFAULT_BODY);
        when(generator.now()).thenReturn(DEFAULT_NOW);
        when(generator.uuid()).thenReturn(DEFAULT_ID);

        dispatcher.dispatchByEmail(DEFAULT_EMAIL, DEFAULT_SUBJECT, DEFAULT_BODY);

        verify(emailSender).send(profile, DEFAULT_SUBJECT, DEFAULT_BODY);
        verify(notificationRepository).save(expectedNotification);
        verify(telegramSender, never()).send(profile, DEFAULT_SUBJECT, DEFAULT_BODY);
    }

    @Test
    @DisplayName("does nothing when user notification profile not found by email")
    void givenUserNotFoundByEmail_dispatchByEmail_shouldDoNothing() {
        when(userApi.getUserNotificationProfileByEmail(DEFAULT_EMAIL)).thenReturn(Optional.empty());

        dispatcher.dispatchByEmail(DEFAULT_EMAIL, DEFAULT_SUBJECT, DEFAULT_BODY);

        verifyNoInteractions(emailSender, telegramSender, notificationRepository, generator);
    }

    @Test
    @DisplayName("dispatchDirect - sends and saves notification directly by email when sender supports profile")
    void givenDirectEmailRecipient_dispatchDirect_shouldSendAndPersist() {
        UserNotificationProfile profile = new UserNotificationProfile(null, "Jane Candidate", "jane@example.com", null);
        Notification expectedNotification = Notification.pending(
                DEFAULT_ID,
                null,
                "jane@example.com",
                null,
                NotificationChannel.EMAIL,
                DEFAULT_SUBJECT,
                DEFAULT_BODY,
                DEFAULT_NOW
        ).markSent(DEFAULT_NOW);

        when(emailSender.supports(profile)).thenReturn(true);
        when(emailSender.getChannel()).thenReturn(NotificationChannel.EMAIL);
        when(telegramSender.supports(profile)).thenReturn(false);
        doNothing().when(emailSender).send(profile, DEFAULT_SUBJECT, DEFAULT_BODY);
        when(generator.now()).thenReturn(DEFAULT_NOW);
        when(generator.uuid()).thenReturn(DEFAULT_ID);

        dispatcher.dispatchDirect("Jane Candidate", "jane@example.com", null, DEFAULT_SUBJECT, DEFAULT_BODY);

        verify(emailSender).send(profile, DEFAULT_SUBJECT, DEFAULT_BODY);
        verify(notificationRepository).save(expectedNotification);
        verifyNoInteractions(userApi);
    }

    @Test
    @DisplayName("dispatchDirect - does nothing when both email and telegram are null or blank")
    void givenNoEmailOrTelegram_dispatchDirect_shouldDoNothing() {
        dispatcher.dispatchDirect("Jane Candidate", null, "   ", DEFAULT_SUBJECT, DEFAULT_BODY);

        verifyNoInteractions(userApi, emailSender, telegramSender, notificationRepository, generator);
    }
}
