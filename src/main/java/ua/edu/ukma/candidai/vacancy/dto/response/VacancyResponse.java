package ua.edu.ukma.candidai.vacancy.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import ua.edu.ukma.candidai.vacancy.model.EmploymentType;
import ua.edu.ukma.candidai.vacancy.model.EnglishLevel;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.LocationType;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Vacancy details response payload")
public record VacancyResponse(
        @Schema(description = "Unique vacancy ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
        UUID authorId,
        UUID assignedRecruiterId,
        UUID companyId,
        String title,
        JobCategory category,
        String specialization,
        String seniorityLevel,
        Integer minYearsOfExperience,
        String description,
        List<String> requiredSkills,
        List<String> preferredSkills,
        EnglishLevel minEnglishLevel,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String currency,
        EmploymentType employmentType,
        LocationType locationType,
        String location,
        VacancyStatus status,
        Instant publishedAt,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {}
