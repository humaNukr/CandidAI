package ua.edu.ukma.candidai.notification.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.edu.ukma.candidai.notification.service.NotificationDispatcher;
import ua.edu.ukma.candidai.recruitment.ApplicationStatusChangedEvent;
import ua.edu.ukma.candidai.recruitment.ApplicationSubmittedEvent;

import static org.mockito.Mockito.verify;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_RECIPIENT_ID;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.expectedApplicationStatusChangedBody;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.expectedApplicationStatusChangedSubject;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.expectedApplicationSubmittedBody;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.expectedApplicationSubmittedSubject;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleApplicationStatusChangedEvent;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleApplicationSubmittedEvent;

@ExtendWith(MockitoExtension.class)
class ApplicationNotificationListenerTest {

    @Mock
    private NotificationDispatcher dispatcher;

    @InjectMocks
    private ApplicationNotificationListener listener;

    @Test
    @DisplayName("dispatches to candidateId when ApplicationSubmittedEvent received")
    void givenApplicationSubmittedEvent_on_shouldDispatchToCandidateId() {
        ApplicationSubmittedEvent event = sampleApplicationSubmittedEvent(DEFAULT_RECIPIENT_ID);

        listener.on(event);

        verify(dispatcher).dispatch(
                DEFAULT_RECIPIENT_ID,
                expectedApplicationSubmittedSubject(event),
                expectedApplicationSubmittedBody(event)
        );
    }

    @Test
    @DisplayName("dispatches to candidateId when ApplicationStatusChangedEvent received")
    void givenApplicationStatusChangedEvent_on_shouldDispatchToCandidateId() {
        ApplicationStatusChangedEvent event = sampleApplicationStatusChangedEvent(DEFAULT_RECIPIENT_ID);

        listener.on(event);

        verify(dispatcher).dispatch(
                DEFAULT_RECIPIENT_ID,
                expectedApplicationStatusChangedSubject(event),
                expectedApplicationStatusChangedBody(event)
        );
    }
}
