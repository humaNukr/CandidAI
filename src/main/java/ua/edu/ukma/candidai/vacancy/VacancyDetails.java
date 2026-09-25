package ua.edu.ukma.candidai.vacancy;

import java.util.List;
import java.util.UUID;

public record VacancyDetails(
        UUID id,
        String title,
        String description,
        List<String> requiredSkills,
        List<String> preferredSkills,
        String seniorityLevel
) {}
