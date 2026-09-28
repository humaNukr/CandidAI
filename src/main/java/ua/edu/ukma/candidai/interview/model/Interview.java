package ua.edu.ukma.candidai.interview.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ua.edu.ukma.candidai.common.exception.InvalidStateTransitionException;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "interviews")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Interview {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "interviewer_id")
    private UUID interviewerId;

    @Column(name = "interviewer_name", nullable = false, length = 120)
    private String interviewerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private InterviewType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InterviewStatus status;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "meeting_link", length = 500)
    private String meetingLink;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Interview create(
            UUID id,
            UUID applicationId,
            UUID interviewerId,
            String interviewerName,
            InterviewType type,
            Instant scheduledAt,
            int durationMinutes,
            String meetingLink,
            String notes,
            Instant now
    ) {
        if (scheduledAt.isBefore(now)) {
            throw new IllegalArgumentException("Cannot schedule interview in the past");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Interview duration must be greater than zero");
        }

        return Interview.builder()
                .id(id)
                .applicationId(applicationId)
                .interviewerId(interviewerId)
                .interviewerName(interviewerName)
                .type(type)
                .status(InterviewStatus.SCHEDULED)
                .scheduledAt(scheduledAt)
                .durationMinutes(durationMinutes)
                .meetingLink(meetingLink)
                .notes(notes)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void reschedule(
            Instant newScheduledAt,
            Integer newDurationMinutes,
            String newMeetingLink,
            Instant now
    ) {
        if (this.status.isTerminal()) {
            throw new InvalidStateTransitionException(
                    "Cannot reschedule interview with terminal status: " + this.status
            );
        }
        if (newScheduledAt.isBefore(now)) {
            throw new IllegalArgumentException("New interview time cannot be in the past");
        }

        this.scheduledAt = newScheduledAt;
        if (newDurationMinutes != null && newDurationMinutes > 0) {
            this.durationMinutes = newDurationMinutes;
        }
        if (newMeetingLink != null && !newMeetingLink.isBlank()) {
            this.meetingLink = newMeetingLink;
        }
        this.status = InterviewStatus.RESCHEDULED;
        this.updatedAt = now;
    }

    public void cancel(String reason, Instant now) {
        if (this.status == InterviewStatus.COMPLETED) {
            throw new InvalidStateTransitionException("Cannot cancel an already completed interview");
        }
        if (this.status == InterviewStatus.CANCELLED) {
            throw new InvalidStateTransitionException("Interview is already cancelled");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Cancellation reason must not be blank");
        }

        this.status = InterviewStatus.CANCELLED;
        this.cancellationReason = reason;
        this.updatedAt = now;
    }

    public void complete(Instant now) {
        if (this.status == InterviewStatus.CANCELLED) {
            throw new InvalidStateTransitionException("Cannot complete a cancelled interview");
        }
        if (this.status == InterviewStatus.COMPLETED) {
            throw new InvalidStateTransitionException("Interview is already completed");
        }

        this.status = InterviewStatus.COMPLETED;
        this.updatedAt = now;
    }
}
