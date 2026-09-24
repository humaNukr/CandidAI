package ua.edu.ukma.candidai.vacancy.model;

import java.time.Instant;
import java.util.UUID;

public record Company(
        UUID id,
        String name,
        String description,
        String logoUrl,
        String contactEmail,
        Instant createdAt
) {}
