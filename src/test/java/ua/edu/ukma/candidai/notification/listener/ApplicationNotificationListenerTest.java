package ua.edu.ukma.candidai.notification.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.edu.ukma.candidai.notification.service.NotificationDispatcher;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.event.ApplicationStatusChangedEvent;
import ua.edu.ukma.candidai.recruitment.event.ApplicationSubmittedEvent;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApplicationNotificationListenerTest {

    private static final UUID APPLICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID VACANCY_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final Instant NOW = Instant.parse("2026-09-24T10:00:00Z");

    @Mock
    private NotificationDispatcher dispatcher;

    @InjectMocks
    private ApplicationNotificationListener listener;

    @Test
    @DisplayName("on(ApplicationSubmittedEvent) - should send confirmation email to candidate")
    void givenApplicationSubmittedEvent_on_shouldDispatchNotification() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent(
                APPLICATION_ID,
                VACANCY_ID,
                "John Doe",
                "john.doe@example.com",
                "https://storage.candidai.ukma.edu.ua/resumes/john.pdf",
                NOW
        );

        listener.on(event);

        verify(dispatcher).dispatchDirectEmail(
                eq("John Doe"),
                eq("john.doe@example.com"),
                eq("Application Received - CandidAI"),
                contains("Thank you for applying")
        );
    }

    @Test
    @DisplayName("on(ApplicationStatusChangedEvent) - should send status update email to candidate")
    void givenApplicationStatusChangedEvent_on_shouldDispatchNotification() {
        ApplicationStatusChangedEvent event = new ApplicationStatusChangedEvent(
                APPLICATION_ID,
                VACANCY_ID,
                "John Doe",
                "john.doe@example.com",
                ApplicationStatus.APPLIED,
                ApplicationStatus.SCREENING,
                "Passed initial filter",
                NOW
        );

        listener.on(event);

        verify(dispatcher).dispatchDirectEmail(
                eq("John Doe"),
                eq("john.doe@example.com"),
                eq("Application Status Update: SCREENING - CandidAI"),
                contains("updated from APPLIED to SCREENING")
        );
    }
}
