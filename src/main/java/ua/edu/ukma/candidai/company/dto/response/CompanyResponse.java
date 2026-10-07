package ua.edu.ukma.candidai.company.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Detailed company profile response")
public record CompanyResponse(
        @Schema(description = "Unique company ID", example = "c0000000-0000-0000-0000-000000000003")
        UUID id,

        @Schema(description = "Company name", example = "SoftServe Tech")
        String name,

        @Schema(description = "Company overview", example = "Global IT consulting and software engineering firm")
        String description,

        @Schema(description = "Company logo URL", example = "https://candidai.ukma.edu.ua/logos/softserve.png")
        String logoUrl,

        @Schema(description = "Contact email", example = "careers@softserveinc.com")
        String contactEmail,

        @Schema(description = "Registration timestamp", example = "2026-09-27T10:46:00Z")
        Instant createdAt,

        @Schema(description = "Number of active open vacancies", example = "5")
        int activeVacanciesCount
) {
}
