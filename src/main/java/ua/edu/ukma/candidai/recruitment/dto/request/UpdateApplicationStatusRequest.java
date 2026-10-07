package ua.edu.ukma.candidai.recruitment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.validation.ValidStatusUpdate;

@Schema(description = "Request payload for updating application status")
@ValidStatusUpdate
public record UpdateApplicationStatusRequest(
        @Schema(description = "Target application status", example = "INTERVIEW")
        @NotNull(message = "Status is required")
        ApplicationStatus status,

        @Schema(description = "Recruiter note or comment", example = "Candidate demonstrated strong backend skills")
        @Size(max = 1000, message = "Comment must not exceed 1000 characters")
        String comment,

        @Schema(description = "Calculated matching score (0-100)", example = "88")
        Integer matchingScore
) {
    public UpdateApplicationStatusRequest(ApplicationStatus status, String comment) {
        this(status, comment, null);
    }
}
