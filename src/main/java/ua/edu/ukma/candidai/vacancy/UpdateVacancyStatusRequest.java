package ua.edu.ukma.candidai.vacancy;

import jakarta.validation.constraints.NotNull;

public record UpdateVacancyStatusRequest(
        @NotNull(message = "Status is required")
        VacancyStatus status
) {}
