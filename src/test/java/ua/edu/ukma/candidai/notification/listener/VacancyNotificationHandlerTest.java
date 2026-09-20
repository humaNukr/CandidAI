package ua.edu.ukma.candidai.notification.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.edu.ukma.candidai.notification.model.NotificationMessage;
import ua.edu.ukma.candidai.notification.service.NotificationDispatcher;
import ua.edu.ukma.candidai.user.UserApi;
import ua.edu.ukma.candidai.user.UserNotificationProfile;
import ua.edu.ukma.candidai.vacancy.VacancyStatusChangedEvent;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_RECIPIENT_ID;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleNotificationMessage;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleUserNotificationProfile;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleVacancyStatusChangedEvent;

@ExtendWith(MockitoExtension.class)
class VacancyNotificationHandlerTest {

    @Mock
    private UserApi userApi;

    @Mock
    private NotificationDispatcher dispatcher;

    @InjectMocks
    private VacancyNotificationHandler handler;

    @Test
    @DisplayName("when user found -> maps profile and dispatches NotificationMessage")
    void givenUserFound_on_shouldMapProfileAndDispatch() {
        VacancyStatusChangedEvent event = sampleVacancyStatusChangedEvent();
        UserNotificationProfile profile = sampleUserNotificationProfile();
        NotificationMessage expectedMessage = sampleNotificationMessage();

        when(userApi.getUserNotificationProfile(DEFAULT_RECIPIENT_ID)).thenReturn(Optional.of(profile));

        handler.on(event);

        verify(userApi).getUserNotificationProfile(DEFAULT_RECIPIENT_ID);
        verify(dispatcher).dispatch(expectedMessage);
    }

    @Test
    @DisplayName("when user not found -> logs warning and does not dispatch")
    void givenUserNotFound_on_shouldLogWarningAndNotDispatch() {
        VacancyStatusChangedEvent event = sampleVacancyStatusChangedEvent();

        when(userApi.getUserNotificationProfile(DEFAULT_RECIPIENT_ID)).thenReturn(Optional.empty());

        handler.on(event);

        verify(userApi).getUserNotificationProfile(DEFAULT_RECIPIENT_ID);
        verifyNoInteractions(dispatcher);
    }
}
