package ua.edu.ukma.candidai.recruitment.dto.response;

import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;

public record ApplicationStatusCount(
        ApplicationStatus status,
        long total
) {
}
