package ua.edu.ukma.candidai.assessment.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.edu.ukma.candidai.assessment.service.AssessmentService;
import ua.edu.ukma.candidai.recruitment.ApplicationSubmittedEvent;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AssessmentEventListenerTest {

    private static final UUID APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID VACANCY_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final Instant NOW = Instant.parse("2026-09-21T10:00:00Z");

    @Mock
    private AssessmentService assessmentService;

    @InjectMocks
    private AssessmentEventListener listener;

    @Test
    @DisplayName("onApplicationSubmitted - should delegate screening to AssessmentService")
    void givenApplicationSubmittedEvent_onApplicationSubmitted_shouldDelegateToAssessmentService() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent(
                APP_ID, VACANCY_ID, "John Doe", "test@example.com", "https://example.com/resume.pdf", NOW
        );

        listener.onApplicationSubmitted(event);

        verify(assessmentService).executeScreening(APP_ID);
    }
}
