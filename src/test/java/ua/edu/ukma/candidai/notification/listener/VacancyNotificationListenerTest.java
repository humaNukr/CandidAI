package ua.edu.ukma.candidai.notification.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.edu.ukma.candidai.notification.service.NotificationDispatcher;
import ua.edu.ukma.candidai.vacancy.VacancyStatusChangedEvent;

import static org.mockito.Mockito.verify;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_RECIPIENT_ID;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.expectedVacancyStatusChangedBody;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.expectedVacancyStatusChangedSubject;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleVacancyStatusChangedEvent;

@ExtendWith(MockitoExtension.class)
class VacancyNotificationListenerTest {

    @Mock
    private NotificationDispatcher dispatcher;

    @InjectMocks
    private VacancyNotificationListener listener;

    @Test
    @DisplayName("formats message and dispatches notification on vacancy status changed event")
    void givenVacancyStatusChangedEvent_on_shouldDispatchFormattedNotification() {
        VacancyStatusChangedEvent event = sampleVacancyStatusChangedEvent();

        listener.on(event);

        verify(dispatcher).dispatch(
                DEFAULT_RECIPIENT_ID,
                expectedVacancyStatusChangedSubject(event),
                expectedVacancyStatusChangedBody(event)
        );
    }
}
