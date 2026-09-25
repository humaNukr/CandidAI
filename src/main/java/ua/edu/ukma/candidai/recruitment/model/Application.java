package ua.edu.ukma.candidai.recruitment.model;

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
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Application {

    @EqualsAndHashCode.Include
    private UUID id;

    private UUID vacancyId;
    private String candidateName;
    private String email;
    private String phone;
    private String resumeUrl;
    private ApplicationStatus status;
    private String comment;
    private Integer matchingScore;
    private Instant appliedAt;
    private Instant updatedAt;

    public static Application create(ApplyForVacancyRequest request, UUID id, Instant now) {
        return Application.builder()
                .id(id)
                .vacancyId(request.vacancyId())
                .candidateName(request.candidateName())
                .email(request.email())
                .phone(request.phone())
                .resumeUrl(request.resumeUrl())
                .status(ApplicationStatus.APPLIED)
                .comment(null)
                .matchingScore(null)
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
