package ua.edu.ukma.candidai.recruitment.service.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.event.ApplicationStatusChangedEvent;
import ua.edu.ukma.candidai.recruitment.event.ApplicationSubmittedEvent;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;

class ApplicationEventListenerTest {

    private static final UUID APPLICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID VACANCY_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final Instant NOW = Instant.parse("2026-09-24T10:00:00Z");

    private final ApplicationEventListener listener = new ApplicationEventListener();

    @Test
    @DisplayName("onApplicationSubmitted - should handle event without exception")
    void givenApplicationSubmittedEvent_onApplicationSubmitted_shouldHandleEvent() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent(
                APPLICATION_ID,
                VACANCY_ID,
                "John Doe",
                "john.doe@example.com",
                "https://storage.candidai.ukma.edu.ua/resumes/john.pdf",
                NOW
        );

        assertThatCode(() -> listener.onApplicationSubmitted(event))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("onApplicationStatusChanged - should handle event without exception")
    void givenApplicationStatusChangedEvent_onApplicationStatusChanged_shouldHandleEvent() {
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

        assertThatCode(() -> listener.onApplicationStatusChanged(event))
                .doesNotThrowAnyException();
    }
}
