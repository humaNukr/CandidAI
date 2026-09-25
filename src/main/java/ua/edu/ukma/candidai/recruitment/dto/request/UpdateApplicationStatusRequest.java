package ua.edu.ukma.candidai.recruitment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.validation.ValidStatusUpdate;

@ValidStatusUpdate
public record UpdateApplicationStatusRequest(
        @NotNull(message = "Status is required")
        ApplicationStatus status,

        @Size(max = 1000, message = "Comment must not exceed 1000 characters")
        String comment,

        Integer matchingScore
) {
    public UpdateApplicationStatusRequest(ApplicationStatus status, String comment) {
        this(status, comment, null);
    }
}
