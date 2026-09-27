package ua.edu.ukma.candidai.recruitment.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ua.edu.ukma.candidai.common.exception.InvalidStateTransitionException;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.request.ApplyForVacancyRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "applications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Application {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "vacancy_id", nullable = false)
    private UUID vacancyId;

    @Column(name = "candidate_id")
    private UUID candidateId;

    @Column(name = "candidate_name", nullable = false, length = 120)
    private String candidateName;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "resume_url", length = 500)
    private String resumeUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStatus status;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "matching_score")
    private Integer matchingScore;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InterviewFeedback> feedbacks = new ArrayList<>();

    public void addFeedback(InterviewFeedback feedback) {
        if (this.feedbacks == null) {
            this.feedbacks = new ArrayList<>();
        }
        this.feedbacks.add(feedback);
        feedback.setApplication(this);
    }

    public void removeFeedback(InterviewFeedback feedback) {
        if (this.feedbacks != null) {
            this.feedbacks.remove(feedback);
            feedback.setApplication(null);
        }
    }

    public static Application create(ApplyForVacancyRequest request, UUID id, Instant now) {
        return Application.builder()
                .id(id)
                .vacancyId(request.vacancyId())
                .candidateId(request.candidateId())
                .candidateName(request.candidateName())
                .email(request.email())
                .phone(request.phone())
                .resumeUrl(request.resumeUrl())
                .status(ApplicationStatus.APPLIED)
                .appliedAt(now)
                .updatedAt(now)
                .build();
    }

    public void updateStatus(ApplicationStatus newStatus, String comment, Instant now) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new InvalidStateTransitionException(
                    "Invalid status transition from " + this.status + " to " + newStatus
            );
        }
        this.status = newStatus;
        this.comment = comment;
        this.updatedAt = now;
    }

    public void updateStatus(ApplicationStatus newStatus, Integer matchingScore, String comment, Instant now) {
        updateStatus(newStatus, comment, now);
        this.matchingScore = matchingScore;
    }

    public boolean isInInterview() {
        return this.status == ApplicationStatus.INTERVIEW;
    }
}
