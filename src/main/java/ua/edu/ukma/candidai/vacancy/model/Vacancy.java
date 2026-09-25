package ua.edu.ukma.candidai.vacancy.model;

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
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vacancy {

    @EqualsAndHashCode.Include
    private UUID id;
    private UUID authorId;
    private UUID assignedRecruiterId;
    private UUID companyId;
    private String title;
    private JobCategory category;
    private String specialization;
    private String seniorityLevel;
    private Integer minYearsOfExperience;
    private String description;
    private List<String> requiredSkills;
    private List<String> preferredSkills;
    private EnglishLevel minEnglishLevel;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String currency;
    private EmploymentType employmentType;
    private LocationType locationType;
    private String location;
    private VacancyStatus status;
    private boolean deleted;
    private Instant deletedAt;
    private Instant publishedAt;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static Vacancy create(CreateVacancyRequest request, UUID id, Instant now) {
        VacancyStatus initialStatus = request.status() != null ? request.status() : VacancyStatus.OPEN;
        if (initialStatus != VacancyStatus.DRAFT && initialStatus != VacancyStatus.OPEN) {
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
                .requiredSkills(request.requiredSkills())
                .preferredSkills(request.preferredSkills())
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
