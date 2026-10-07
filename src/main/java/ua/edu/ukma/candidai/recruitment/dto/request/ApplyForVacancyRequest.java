package ua.edu.ukma.candidai.recruitment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.util.UUID;

@Schema(description = "Request payload for applying to a vacancy")
public record ApplyForVacancyRequest(
        @Schema(description = "Target vacancy ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull(message = "Vacancy ID is required")
        UUID vacancyId,

        @Schema(description = "Applicant candidate ID", example = "c0000000-0000-0000-0000-000000000001")
        @NotNull(message = "Candidate ID is required")
        UUID candidateId,

        @Schema(description = "Full name of candidate", example = "Jane Doe")
        @NotBlank(message = "Candidate name is required")
        @Size(max = 100, message = "Candidate name must not exceed 100 characters")
        String candidateName,

        @Schema(description = "Candidate contact email", example = "jane.doe@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @Schema(description = "Candidate phone number in international format", example = "+380501234567")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
        String phone,

        @Schema(
                description = "Direct URL to candidate resume (PDF/DOCX)",
                example = "https://storage.candidai.ukma.edu.ua/resumes/jane_doe.pdf"
        )
        @NotBlank(message = "Resume URL is required")
        @URL(message = "Invalid resume URL format")
        @Size(max = 500, message = "Resume URL must not exceed 500 characters")
        String resumeUrl
) {
}
