package ua.edu.ukma.candidai.company.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CompanySummaryResponse(
        UUID id,
        String name,
        String description,
        String logoUrl,
        String contactEmail,
        Instant createdAt
) {
}
