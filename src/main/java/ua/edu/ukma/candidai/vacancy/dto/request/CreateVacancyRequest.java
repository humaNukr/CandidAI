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

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Request payload for creating a new vacancy")
@ValidSalaryRange
public record CreateVacancyRequest(
        @Schema(description = "Unique ID of the vacancy author", example = "a0000000-0000-0000-0000-000000000001")
        @NotNull(message = "Author ID is required")
        UUID authorId,

        @Schema(description = "Assigned recruiter ID", example = "b0000000-0000-0000-0000-000000000002")
        UUID assignedRecruiterId,

        @Schema(description = "Company ID", example = "c0000000-0000-0000-0000-000000000003")
        UUID companyId,

        @Schema(description = "Initial vacancy status", example = "OPEN")
        VacancyStatus status,

        @Schema(description = "Job title", example = "Senior Java Engineer")
        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 150, message = "Title must be between 3 and 150 characters")
        String title,

        @Schema(description = "Job category", example = "ENGINEERING")
        @NotNull(message = "Job category is required")
        JobCategory category,

        @Schema(description = "Job specialization", example = "Java / Spring Boot")
        @NotBlank(message = "Specialization is required")
        @Size(min = 2, max = 100, message = "Specialization must be between 2 and 100 characters")
        String specialization,

        @Schema(description = "Seniority level", example = "Senior")
        @NotBlank(message = "Seniority level is required")
        @Size(min = 2, max = 50, message = "Seniority level must be between 2 and 50 characters")
        String seniorityLevel,

        @Schema(description = "Minimum years of experience", example = "5")
        @NotNull(message = "Minimum years of experience is required")
        @Min(value = 0, message = "Minimum years of experience cannot be negative")
        @Max(value = 50, message = "Minimum years of experience cannot exceed 50")
        Integer minYearsOfExperience,

        @Schema(description = "Job description", example = "Seeking a Senior Java Engineer to lead AI hiring platform.")
        @NotBlank(message = "Description is required")
        @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
        String description,

        @Schema(description = "List of required skills", example = "[\"Java 25\", \"Spring Boot\", \"PostgreSQL\"]")
        @NotEmpty(message = "At least one required skill must be specified")
        List<@NotBlank String> requiredSkills,

        @Schema(description = "List of preferred skills", example = "[\"Docker\", \"Kubernetes\"]")
        List<String> preferredSkills,

        @Schema(description = "Minimum English proficiency", example = "B2")
        @NotNull(message = "Minimum English level is required")
        EnglishLevel minEnglishLevel,

        @Schema(description = "Minimum salary offer", example = "4500.00")
        @Positive(message = "Minimum salary must be positive")
        BigDecimal salaryMin,

        @Schema(description = "Maximum salary offer", example = "6500.00")
        @Positive(message = "Maximum salary must be positive")
        BigDecimal salaryMax,

        @Schema(description = "Currency ISO code", example = "USD")
        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency code must be 3 characters (e.g. USD)")
        String currency,

        @Schema(description = "Employment type", example = "FULL_TIME")
        @NotNull(message = "Employment type is required")
        EmploymentType employmentType,

        @Schema(description = "Work location type", example = "REMOTE")
        @NotNull(message = "Location type is required")
        LocationType locationType,

        @Schema(description = "Geographic location", example = "Kyiv, Ukraine")
        @Size(max = 150, message = "Location must not exceed 150 characters")
        String location,

        @Schema(description = "Vacancy expiration timestamp", example = "2026-12-31T23:59:59Z")
        @Future(message = "Expiration date must be in the future")
        Instant expiresAt
) {}
