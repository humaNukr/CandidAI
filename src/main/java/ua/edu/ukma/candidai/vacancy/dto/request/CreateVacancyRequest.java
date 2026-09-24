package ua.edu.ukma.candidai.vacancy.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ua.edu.ukma.candidai.vacancy.model.EmploymentType;
import ua.edu.ukma.candidai.vacancy.model.EnglishLevel;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.LocationType;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;
import ua.edu.ukma.candidai.vacancy.validation.ValidSalaryRange;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ValidSalaryRange
public record CreateVacancyRequest(
        @NotNull(message = "Author ID is required")
        UUID authorId,

        UUID assignedRecruiterId,

        UUID companyId,

        VacancyStatus status,

        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 150, message = "Title must be between 3 and 150 characters")
        String title,

        @NotNull(message = "Job category is required")
        JobCategory category,

        @NotBlank(message = "Specialization is required")
        @Size(min = 2, max = 100, message = "Specialization must be between 2 and 100 characters")
        String specialization,

        @NotBlank(message = "Seniority level is required")
        @Size(min = 2, max = 50, message = "Seniority level must be between 2 and 50 characters")
        String seniorityLevel,

        @NotNull(message = "Minimum years of experience is required")
        @Min(value = 0, message = "Minimum years of experience cannot be negative")
        @Max(value = 50, message = "Minimum years of experience cannot exceed 50")
        Integer minYearsOfExperience,

        @NotBlank(message = "Description is required")
        @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
        String description,

        @NotEmpty(message = "At least one required skill must be specified")
        List<@NotBlank String> requiredSkills,

        List<String> preferredSkills,

        @NotNull(message = "Minimum English level is required")
        EnglishLevel minEnglishLevel,

        @Positive(message = "Minimum salary must be positive")
        BigDecimal salaryMin,

        @Positive(message = "Maximum salary must be positive")
        BigDecimal salaryMax,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency code must be 3 characters (e.g. USD)")
        String currency,

        @NotNull(message = "Employment type is required")
        EmploymentType employmentType,

        @NotNull(message = "Location type is required")
        LocationType locationType,

        @Size(max = 150, message = "Location must not exceed 150 characters")
        String location,

        @Future(message = "Expiration date must be in the future")
        Instant expiresAt
) {}
