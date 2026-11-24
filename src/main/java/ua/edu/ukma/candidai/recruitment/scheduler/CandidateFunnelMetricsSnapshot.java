package ua.edu.ukma.candidai.recruitment.scheduler;

import java.time.Instant;
import java.util.Map;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;

public record CandidateFunnelMetricsSnapshot(
        Instant calculatedAt,
        long totalApplications,
        Map<ApplicationStatus, Long> countsByStatus,
        double hireRatePercent,
        Double averageMatchingScore
) {
}
