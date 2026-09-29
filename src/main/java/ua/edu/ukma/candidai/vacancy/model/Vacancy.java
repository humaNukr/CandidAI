package ua.edu.ukma.candidai.vacancy.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ua.edu.ukma.candidai.common.exception.InvalidStateTransitionException;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vacancies")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vacancy {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    private UUID authorId;

    private UUID assignedRecruiterId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    private String title;

    @Enumerated(EnumType.STRING)
    private JobCategory category;

    private String specialization;

    private String seniorityLevel;

    private Integer minYearsOfExperience;

    private String description;

    @Builder.Default
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "vacancy_skills",
            joinColumns = @JoinColumn(name = "vacancy_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private List<Skill> skills = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private EnglishLevel minEnglishLevel;

    private BigDecimal salaryMin;

    private BigDecimal salaryMax;

    private String currency;

    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    private LocationType locationType;

    private String location;

    @Enumerated(EnumType.STRING)
    private VacancyStatus status;

    private boolean deleted;

    private Instant deletedAt;

    private Instant publishedAt;

    private Instant expiresAt;

    private Instant createdAt;

    private Instant updatedAt;

    public static Vacancy create(CreateVacancyRequest request, UUID id, Instant now) {
        return create(request, id, now, List.of());
    }

    public static Vacancy create(CreateVacancyRequest request, UUID id, Instant now, List<Skill> skills) {
        VacancyStatus initialStatus = request.status() != null ? request.status() : VacancyStatus.OPEN;
        if (!initialStatus.isInitial()) {
            throw new InvalidStateTransitionException(
                    "Initial vacancy status must be DRAFT or OPEN, got: " + initialStatus
            );
        }
        Instant publishedAt = (initialStatus == VacancyStatus.OPEN) ? now : null;

        return Vacancy.builder()
                .id(id)
                .authorId(request.authorId())
                .assignedRecruiterId(request.assignedRecruiterId())
                .companyId(request.companyId())
                .title(request.title())
                .category(request.category())
                .specialization(request.specialization())
                .seniorityLevel(request.seniorityLevel())
                .minYearsOfExperience(request.minYearsOfExperience())
                .description(request.description())
                .skills(skills != null ? new ArrayList<>(skills) : new ArrayList<>())
                .minEnglishLevel(request.minEnglishLevel())
                .salaryMin(request.salaryMin())
                .salaryMax(request.salaryMax())
                .currency(request.currency())
                .employmentType(request.employmentType())
                .locationType(request.locationType())
                .location(request.location())
                .status(initialStatus)
                .deleted(false)
                .publishedAt(publishedAt)
                .expiresAt(request.expiresAt())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void addSkill(Skill skill) {
        if (skill != null) {
            if (this.skills == null) {
                this.skills = new ArrayList<>();
            }
            if (!this.skills.contains(skill)) {
                this.skills.add(skill);
            }
        }
    }

    public void removeSkill(Skill skill) {
        if (skill != null && this.skills != null) {
            this.skills.remove(skill);
        }
    }

    public List<String> getSkillNames() {
        if (this.skills == null) {
            return List.of();
        }
        return this.skills.stream()
                .map(Skill::getName)
                .toList();
    }

    public void updateStatus(VacancyStatus status, Instant updatedAt) {
        if (!this.status.canTransitionTo(status)) {
            throw new InvalidStateTransitionException(
                    "Cannot transition vacancy status from " + this.status + " to " + status
            );
        }
        if (this.status == VacancyStatus.DRAFT && status == VacancyStatus.OPEN) {
            this.publishedAt = updatedAt;
        }
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public void softDelete(Instant deletedAt) {
        this.deleted = true;
        this.deletedAt = deletedAt;
        this.updatedAt = deletedAt;
    }
}
