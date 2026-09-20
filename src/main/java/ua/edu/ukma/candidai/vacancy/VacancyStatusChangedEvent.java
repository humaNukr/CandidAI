package ua.edu.ukma.candidai.vacancy;

import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;

import java.time.Instant;
import java.util.UUID;

public record VacancyStatusChangedEvent(
        UUID vacancyId,
        String vacancyTitle,
        UUID authorId,
        VacancyStatus oldStatus,
        VacancyStatus newStatus,
        Instant timestamp
) {}
