package ua.edu.ukma.candidai.recruitment.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.model.Application;
import ua.edu.ukma.candidai.recruitment.repository.ApplicationRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class StaleApplicationsScheduler {

    private static final List<ApplicationStatus> STALE_ELIGIBLE_STATUSES = List.of(
            ApplicationStatus.APPLIED,
            ApplicationStatus.SCREENING
    );

    private static final String AUTO_REJECT_COMMENT = "Automatically closed due to inactivity";

    private final ApplicationRepository applicationRepository;
    private final CommonGenerator commonGenerator;

    @Value("${candidai.scheduler.stale-applications.inactivity-days:30}")
    private int inactivityDays;

    @Scheduled(
            cron = "${candidai.scheduler.stale-applications.cron:0 0 4 * * ?}",
            zone = "Europe/Kyiv"
    )
    @SchedulerLock(name = "StaleApplicationsJob", lockAtLeastFor = "PT1M", lockAtMostFor = "PT10M")
    @Transactional
    public void archiveStaleApplications() {
        Instant now = commonGenerator.now();
        Instant cutoff = now.minus(Duration.ofDays(inactivityDays));

        List<Application> staleApplications = applicationRepository
                .findByStatusInAndUpdatedAtBefore(STALE_ELIGIBLE_STATUSES, cutoff);

        for (Application application : staleApplications) {
            application.updateStatus(ApplicationStatus.REJECTED, AUTO_REJECT_COMMENT, now);
        }

        if (!staleApplications.isEmpty()) {
            applicationRepository.saveAll(staleApplications);
        }

        log.info("[SCHEDULER] StaleApplicationsJob: archived {} stale application(s) inactive since before {}",
                staleApplications.size(), cutoff);
    }
}
