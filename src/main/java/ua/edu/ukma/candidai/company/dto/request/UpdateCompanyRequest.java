package ua.edu.ukma.candidai.company.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for updating company details")
public record UpdateCompanyRequest(
        @Schema(description = "Updated company name", example = "SoftServe Ukraine")
        @NotBlank @Size(max = 120) String name,

        @Schema(description = "Updated company description", example = "Leading software engineering consultancy")
        String description,

        @Schema(description = "Updated logo URL", example = "https://candidai.ukma.edu.ua/logos/softserve_new.png")
        @Size(max = 255) String logoUrl,

        @Schema(description = "Updated contact email", example = "hr@softserveinc.com")
        @Email @Size(max = 120) String contactEmail
) {
}
