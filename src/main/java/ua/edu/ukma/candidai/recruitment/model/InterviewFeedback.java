package ua.edu.ukma.candidai.recruitment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ua.edu.ukma.candidai.recruitment.dto.model.InterviewDecision;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "interview_feedbacks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class InterviewFeedback {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "interviewer_id")
    private UUID interviewerId;

    @Column(name = "interviewer_name", nullable = false, length = 120)
    private String interviewerName;

    @Column(name = "technical_score", nullable = false)
    private Integer technicalScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewDecision decision;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
