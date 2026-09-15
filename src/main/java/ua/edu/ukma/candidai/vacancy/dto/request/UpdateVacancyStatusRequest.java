package ua.edu.ukma.candidai.vacancy.dto.request;

import jakarta.validation.constraints.NotNull;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;

public record UpdateVacancyStatusRequest(
        @NotNull(message = "Status is required")
        VacancyStatus status
) {}
