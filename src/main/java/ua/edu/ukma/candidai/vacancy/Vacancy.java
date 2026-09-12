package ua.edu.ukma.candidai.vacancy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
class Vacancy {

    @EqualsAndHashCode.Include
    private UUID id;
    private UUID authorId;
    private UUID assignedRecruiterId;
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
}
