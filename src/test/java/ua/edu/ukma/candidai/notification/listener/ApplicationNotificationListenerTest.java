package ua.edu.ukma.candidai.notification.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.edu.ukma.candidai.notification.service.NotificationDispatcher;
import ua.edu.ukma.candidai.recruitment.event.ApplicationStatusChangedEvent;
import ua.edu.ukma.candidai.recruitment.event.ApplicationSubmittedEvent;

import static org.mockito.Mockito.verify;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_EMAIL;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_FULL_NAME;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.expectedApplicationStatusChangedBody;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.expectedApplicationStatusChangedSubject;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.expectedApplicationSubmittedBody;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.expectedApplicationSubmittedSubject;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleApplicationStatusChangedEvent;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleApplicationStatusChangedEventWithoutComment;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleApplicationSubmittedEvent;

@ExtendWith(MockitoExtension.class)
class ApplicationNotificationListenerTest {

    @Mock
    private NotificationDispatcher dispatcher;

    @InjectMocks
    private ApplicationNotificationListener listener;

    @Test
    @DisplayName("dispatches direct confirmation email on application submitted event")
    void givenApplicationSubmittedEvent_on_shouldDispatchDirectConfirmation() {
        ApplicationSubmittedEvent event = sampleApplicationSubmittedEvent();

        listener.on(event);

        verify(dispatcher).dispatchDirect(
                DEFAULT_EMAIL,
                null,
                DEFAULT_FULL_NAME,
                expectedApplicationSubmittedSubject(event),
                expectedApplicationSubmittedBody(event)
        );
    }

    @Test
    @DisplayName("dispatches direct status update notification with comment on application status changed event")
    void givenApplicationStatusChangedEvent_on_shouldDispatchDirectStatusUpdate() {
        ApplicationStatusChangedEvent event = sampleApplicationStatusChangedEvent();

        listener.on(event);

        verify(dispatcher).dispatchDirect(
                DEFAULT_EMAIL,
                null,
                "Candidate",
                expectedApplicationStatusChangedSubject(event),
                expectedApplicationStatusChangedBody(event)
        );
    }

    @Test
    @DisplayName("dispatches direct status update notification without comment on application status changed event")
    void givenApplicationStatusChangedEventWithoutComment_on_shouldDispatchDirectStatusUpdateWithoutComment() {
        ApplicationStatusChangedEvent event = sampleApplicationStatusChangedEventWithoutComment();

        listener.on(event);

        verify(dispatcher).dispatchDirect(
                DEFAULT_EMAIL,
                null,
                "Candidate",
                expectedApplicationStatusChangedSubject(event),
                expectedApplicationStatusChangedBody(event)
        );
    }
}
