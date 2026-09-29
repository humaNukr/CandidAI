package ua.edu.ukma.candidai.company.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCompanyRequest(
        @NotBlank @Size(max = 120) String name,
        String description,
        @Size(max = 255) String logoUrl,
        @Email @Size(max = 120) String contactEmail
) {
}
