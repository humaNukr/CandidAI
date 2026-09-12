package ua.edu.ukma.candidai.vacancy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VacancyResponse(
        UUID id,
        UUID authorId,
        UUID assignedRecruiterId,
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
