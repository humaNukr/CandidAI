package ua.edu.ukma.candidai.company.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for creating a new company")
public record CreateCompanyRequest(
        @Schema(description = "Company legal or brand name", example = "SoftServe Tech")
        @NotBlank @Size(max = 120) String name,

        @Schema(description = "Brief company overview", example = "Global IT consulting and software engineering firm")
        String description,

        @Schema(description = "URL to company logo image", example = "https://candidai.ukma.edu.ua/logos/softserve.png")
        @Size(max = 255) String logoUrl,

        @Schema(description = "Official contact or HR email", example = "careers@softserveinc.com")
        @Email @Size(max = 120) String contactEmail
) {
}
