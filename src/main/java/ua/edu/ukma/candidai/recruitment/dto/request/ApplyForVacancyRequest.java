package ua.edu.ukma.candidai.recruitment.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ApplyForVacancyRequest(
        @NotNull(message = "Vacancy ID is required")
        UUID vacancyId,

        @NotBlank(message = "Candidate name is required")
        @Size(max = 100, message = "Candidate name must not exceed 100 characters")
        String candidateName,

        @Email(message = "Invalid email format")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
        String phone,

        @NotBlank(message = "Resume URL is required")
        @Size(max = 500, message = "Resume URL must not exceed 500 characters")
        String resumeUrl
        ) {
}
