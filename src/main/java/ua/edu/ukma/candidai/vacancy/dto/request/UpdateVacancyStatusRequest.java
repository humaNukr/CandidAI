package ua.edu.ukma.candidai.vacancy.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;

@Schema(description = "Request payload for updating vacancy status")
public record UpdateVacancyStatusRequest(
        @Schema(description = "New vacancy status", example = "CLOSED")
        @NotNull(message = "Status is required")
        VacancyStatus status
) {}
