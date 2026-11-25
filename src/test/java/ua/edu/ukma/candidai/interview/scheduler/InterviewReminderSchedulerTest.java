package ua.edu.ukma.candidai.interview.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.interview.event.InterviewReminderEvent;
import ua.edu.ukma.candidai.interview.model.Interview;
import ua.edu.ukma.candidai.interview.model.InterviewStatus;
import ua.edu.ukma.candidai.interview.model.InterviewType;
import ua.edu.ukma.candidai.interview.repository.InterviewRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for InterviewReminderScheduler")
class InterviewReminderSchedulerTest {

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CommonGenerator commonGenerator;

    @InjectMocks
    private InterviewReminderScheduler scheduler;

    private final Instant now = Instant.parse("2026-11-21T10:00:00Z");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "windowHours", 24);
        when(commonGenerator.now()).thenReturn(now);
    }

    @Test
    @DisplayName("sendUpcomingInterviewReminders - should dispatch reminder and mark sent when pending interview found")
    void sendUpcomingInterviewReminders_WhenPendingInterview_ShouldDispatchAndMarkSent() {
        Instant scheduledAt = now.plus(Duration.ofHours(5));
        Interview interview = Interview.builder()
                .id(UUID.randomUUID())
                .applicationId(UUID.randomUUID())
                .interviewerId(UUID.randomUUID())
                .interviewerName("Tech Lead John")
                .type(InterviewType.TECHNICAL)
                .status(InterviewStatus.SCHEDULED)
                .scheduledAt(scheduledAt)
                .durationMinutes(60)
                .meetingLink("https://meet.candidai.com/tech-1")
                .createdAt(now.minus(Duration.ofDays(1)))
                .updatedAt(now.minus(Duration.ofDays(1)))
                .build();

        when(interviewRepository.findUpcomingPendingReminders(
                any(),
                eq(now),
                eq(now.plus(Duration.ofHours(24)))
        )).thenReturn(List.of(interview));

        scheduler.sendUpcomingInterviewReminders();

        assertThat(interview.isReminderSent()).isTrue();
        assertThat(interview.getReminderSentAt()).isEqualTo(now);

        ArgumentCaptor<InterviewReminderEvent> captor = ArgumentCaptor.forClass(InterviewReminderEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        InterviewReminderEvent event = captor.getValue();
        assertThat(event.interviewId()).isEqualTo(interview.getId());
        assertThat(event.applicationId()).isEqualTo(interview.getApplicationId());
        assertThat(event.interviewerName()).isEqualTo("Tech Lead John");
        assertThat(event.scheduledAt()).isEqualTo(scheduledAt);
        assertThat(event.meetingLink()).isEqualTo("https://meet.candidai.com/tech-1");
    }

    @Test
    @DisplayName("sendUpcomingInterviewReminders - should do nothing when no pending interviews found")
    void sendUpcomingInterviewReminders_WhenNoInterviews_ShouldDoNothing() {
        when(interviewRepository.findUpcomingPendingReminders(
                any(),
                eq(now),
                eq(now.plus(Duration.ofHours(24)))
        )).thenReturn(List.of());

        scheduler.sendUpcomingInterviewReminders();

        verify(eventPublisher, never()).publishEvent(any());
    }
}
