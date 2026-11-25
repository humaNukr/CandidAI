package ua.edu.ukma.candidai.recruitment.scheduler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationStatusCount;
import ua.edu.ukma.candidai.recruitment.repository.ApplicationFunnelRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class CandidateFunnelMetricsScheduler {

    private static final double PERCENT_SCALE = 100.0;

    private final ApplicationFunnelRepository funnelRepository;
    private final CommonGenerator commonGenerator;
    private final AtomicReference<CandidateFunnelMetricsSnapshot> latestSnapshot = new AtomicReference<>();

    @Scheduled(
            fixedDelayString = "${candidai.scheduler.funnel-metrics.fixed-delay-seconds:900}",
            initialDelayString = "${candidai.scheduler.funnel-metrics.initial-delay-seconds:900}",
            timeUnit = TimeUnit.SECONDS
    )
    @SchedulerLock(
            name = "CandidateFunnelMetricsJob",
            lockAtLeastFor = "${candidai.scheduler.funnel-metrics.lock-at-least-for:PT10S}",
            lockAtMostFor = "${candidai.scheduler.funnel-metrics.lock-at-most-for:PT2M}"
    )
    @Transactional(readOnly = true)
    public void calculateFunnelMetrics() {
        Instant now = commonGenerator.now();
        List<ApplicationStatusCount> statusCounts = funnelRepository.countApplicationsByStatus();
        Double averageMatchingScore = funnelRepository.findAverageMatchingScore();

        Map<ApplicationStatus, Long> countsMap = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            countsMap.put(status, 0L);
        }

        long totalApplications = 0L;
        for (ApplicationStatusCount entry : statusCounts) {
            countsMap.put(entry.status(), entry.total());
            totalApplications += entry.total();
        }

        long hiredCount = countsMap.getOrDefault(ApplicationStatus.HIRED, 0L);
        double hireRatePercent = totalApplications == 0L
                ? 0.0
                : roundTwoDecimals((hiredCount * PERCENT_SCALE) / totalApplications);
        Double roundedAverageScore = averageMatchingScore == null
                ? null
                : roundTwoDecimals(averageMatchingScore);

        CandidateFunnelMetricsSnapshot snapshot = new CandidateFunnelMetricsSnapshot(
                now,
                totalApplications,
                Collections.unmodifiableMap(countsMap),
                hireRatePercent,
                roundedAverageScore
        );
        latestSnapshot.set(snapshot);

        log.info(
                "[SCHEDULER] CandidateFunnelMetricsJob: snapshot at {} "
                        + "(total={}, applied={}, screening={}, interview={}, offer={}, hired={}, rejected={}, "
                        + "avgScore={}, hireRate={}%)",
                now,
                totalApplications,
                countsMap.get(ApplicationStatus.APPLIED),
                countsMap.get(ApplicationStatus.SCREENING),
                countsMap.get(ApplicationStatus.INTERVIEW),
                countsMap.get(ApplicationStatus.OFFER),
                hiredCount,
                countsMap.get(ApplicationStatus.REJECTED),
                roundedAverageScore,
                hireRatePercent
        );
    }

    public Optional<CandidateFunnelMetricsSnapshot> getLatestSnapshot() {
        return Optional.ofNullable(latestSnapshot.get());
    }

    private double roundTwoDecimals(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
