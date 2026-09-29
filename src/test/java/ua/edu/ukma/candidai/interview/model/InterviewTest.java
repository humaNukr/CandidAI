package ua.edu.ukma.candidai.interview.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import ua.edu.ukma.candidai.common.exception.InvalidStateTransitionException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterviewTest {

    private UUID interviewId;
    private UUID applicationId;
    private UUID interviewerId;
    private Instant now;
    private Instant scheduledTime;

    @BeforeEach
    void setUp() {
        interviewId = UUID.randomUUID();
        applicationId = UUID.randomUUID();
        interviewerId = UUID.randomUUID();
        now = Instant.parse("2026-09-28T12:00:00Z");
        scheduledTime = now.plus(1, ChronoUnit.DAYS);
    }

    @Test
    @DisplayName("create should successfully initialize interview with SCHEDULED status")
    void givenValidData_create_shouldInitializeInterviewWithScheduledStatus() {
        Interview interview = Interview.create(
                interviewId,
                applicationId,
                interviewerId,
                "Alex TechLead",
                InterviewType.TECHNICAL,
                scheduledTime,
                60,
                "https://meet.google.com/abc-defg-hij",
                "Technical system design and coding",
                now
        );

        assertThat(interview.getId()).isEqualTo(interviewId);
        assertThat(interview.getApplicationId()).isEqualTo(applicationId);
        assertThat(interview.getInterviewerId()).isEqualTo(interviewerId);
        assertThat(interview.getInterviewerName()).isEqualTo("Alex TechLead");
        assertThat(interview.getType()).isEqualTo(InterviewType.TECHNICAL);
        assertThat(interview.getStatus()).isEqualTo(InterviewStatus.SCHEDULED);
        assertThat(interview.getScheduledAt()).isEqualTo(scheduledTime);
        assertThat(interview.getDurationMinutes()).isEqualTo(60);
        assertThat(interview.getMeetingLink()).isEqualTo("https://meet.google.com/abc-defg-hij");
        assertThat(interview.getNotes()).isEqualTo("Technical system design and coding");
        assertThat(interview.getCreatedAt()).isEqualTo(now);
        assertThat(interview.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("create should throw IllegalArgumentException when scheduledAt is in the past")
    void givenPastTime_create_shouldThrowIllegalArgumentException() {
        Instant pastTime = now.minus(1, ChronoUnit.HOURS);

        assertThatThrownBy(() -> Interview.create(
                interviewId,
                applicationId,
                interviewerId,
                "Alex",
                InterviewType.HR_SCREENING,
                pastTime,
                45,
                null,
                null,
                now
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot schedule interview in the past");
    }

    @Test
    @DisplayName("create should throw IllegalArgumentException when duration is zero or negative")
    void givenZeroOrNegativeDuration_create_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> Interview.create(
                interviewId,
                applicationId,
                interviewerId,
                "Alex",
                InterviewType.HR_SCREENING,
                scheduledTime,
                0,
                null,
                null,
                now
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Interview duration must be greater than zero");
    }

    @Test
    @DisplayName("reschedule should update time, duration, link and status to RESCHEDULED")
    void givenValidNewTime_reschedule_shouldUpdateFieldsAndChangeStatus() {
        Interview interview = createDefaultInterview();
        Instant newTime = scheduledTime.plus(2, ChronoUnit.DAYS);
        Instant rescheduleActionTime = now.plus(1, ChronoUnit.HOURS);

        interview.reschedule(
                newTime, 90, "https://meet.google.com/new-link",
                "Candidate requested later time", rescheduleActionTime
        );

        assertThat(interview.getStatus()).isEqualTo(InterviewStatus.RESCHEDULED);
        assertThat(interview.getScheduledAt()).isEqualTo(newTime);
        assertThat(interview.getDurationMinutes()).isEqualTo(90);
        assertThat(interview.getMeetingLink()).isEqualTo("https://meet.google.com/new-link");
        assertThat(interview.getNotes()).contains("Reschedule reason: Candidate requested later time");
        assertThat(interview.getUpdatedAt()).isEqualTo(rescheduleActionTime);
    }

    @Test
    @DisplayName("reschedule should throw InvalidStateTransitionException when interview is completed or cancelled")
    void givenTerminalInterview_reschedule_shouldThrowInvalidStateTransitionException() {
        Interview completedInterview = createDefaultInterview();
        completedInterview.complete(now);

        Instant newTime = scheduledTime.plus(1, ChronoUnit.DAYS);

        assertThatThrownBy(() -> completedInterview.reschedule(newTime, 60, null, null, now))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot reschedule interview with terminal status: COMPLETED");

        Interview cancelledInterview = createDefaultInterview();
        cancelledInterview.cancel("Candidate declined", now);

        assertThatThrownBy(() -> cancelledInterview.reschedule(newTime, 60, null, null, now))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot reschedule interview with terminal status: CANCELLED");
    }

    @Test
    @DisplayName("reschedule should throw IllegalArgumentException when new scheduled time is in the past")
    void givenPastNewTime_reschedule_shouldThrowIllegalArgumentException() {
        Interview interview = createDefaultInterview();
        Instant actionTime = now.plus(2, ChronoUnit.HOURS);
        Instant pastTime = now.plus(1, ChronoUnit.HOURS); // Before actionTime

        assertThatThrownBy(() -> interview.reschedule(pastTime, 60, null, null, actionTime))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("New interview time cannot be in the past");
    }

    @Test
    @DisplayName("reschedule should throw IllegalArgumentException when duration is zero or negative")
    void givenZeroOrNegativeDuration_reschedule_shouldThrowIllegalArgumentException() {
        Interview interview = createDefaultInterview();
        Instant newTime = scheduledTime.plus(1, ChronoUnit.DAYS);

        assertThatThrownBy(() -> interview.reschedule(newTime, 0, null, null, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Interview duration must be greater than zero");

        assertThatThrownBy(() -> interview.reschedule(newTime, -10, null, null, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Interview duration must be greater than zero");
    }

    @Test
    @DisplayName("reschedule should append reschedule reason to notes when reason is provided")
    void givenReason_reschedule_shouldAppendReasonToNotes() {
        Interview interview = createDefaultInterview();
        Instant newTime = scheduledTime.plus(1, ChronoUnit.DAYS);

        interview.reschedule(newTime, 45, null, "Interviewer sick", now);

        assertThat(interview.getNotes()).contains("Reschedule reason: Interviewer sick");
    }

    @Test
    @DisplayName("cancel should change status to CANCELLED and set cancellationReason")
    void givenValidReason_cancel_shouldChangeStatusAndSetReason() {
        Interview interview = createDefaultInterview();
        Instant cancelTime = now.plus(30, ChronoUnit.MINUTES);

        interview.cancel("Interviewer unavailable", cancelTime);

        assertThat(interview.getStatus()).isEqualTo(InterviewStatus.CANCELLED);
        assertThat(interview.getCancellationReason()).isEqualTo("Interviewer unavailable");
        assertThat(interview.getUpdatedAt()).isEqualTo(cancelTime);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("cancel should throw IllegalArgumentException when reason is null or blank")
    void givenBlankReason_cancel_shouldThrowIllegalArgumentException(String reason) {
        Interview interview = createDefaultInterview();

        assertThatThrownBy(() -> interview.cancel(reason, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cancellation reason must not be blank");
    }

    @Test
    @DisplayName("cancel should throw InvalidStateTransitionException when already cancelled or completed")
    void givenTerminalStatus_cancel_shouldThrowInvalidStateTransitionException() {
        Interview interview = createDefaultInterview();
        interview.cancel("Initial reason", now);

        assertThatThrownBy(() -> interview.cancel("Second reason", now))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessage("Interview is already cancelled");

        Interview completed = createDefaultInterview();
        completed.complete(now);

        assertThatThrownBy(() -> completed.cancel("Reason", now))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessage("Cannot cancel an already completed interview");
    }

    @Test
    @DisplayName("complete should change status to COMPLETED")
    void givenScheduledInterview_complete_shouldChangeStatusToCompleted() {
        Interview interview = createDefaultInterview();
        Instant completeTime = now.plus(2, ChronoUnit.HOURS);

        interview.complete(completeTime);

        assertThat(interview.getStatus()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(interview.getUpdatedAt()).isEqualTo(completeTime);
    }

    @Test
    @DisplayName("complete should throw InvalidStateTransitionException when already completed or cancelled")
    void givenTerminalStatus_complete_shouldThrowInvalidStateTransitionException() {
        Interview completed = createDefaultInterview();
        completed.complete(now);

        assertThatThrownBy(() -> completed.complete(now))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessage("Interview is already completed");

        Interview cancelled = createDefaultInterview();
        cancelled.cancel("Cancelled", now);

        assertThatThrownBy(() -> cancelled.complete(now))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessage("Cannot complete a cancelled interview");
    }

    private Interview createDefaultInterview() {
        return Interview.create(
                interviewId,
                applicationId,
                interviewerId,
                "Alex TechLead",
                InterviewType.TECHNICAL,
                scheduledTime,
                60,
                "https://meet.google.com/test",
                "Notes",
                now
        );
    }
}
