package ua.edu.ukma.candidai.common.audit.scheduler;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.candidai.common.audit.repository.AuditLogRepository;
import ua.edu.ukma.candidai.common.util.CommonGenerator;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditCleanupScheduler {

    private final AuditLogRepository auditLogRepository;
    private final CommonGenerator commonGenerator;

    @Value("${candidai.audit.retention-days}")
    private int retentionDays;

    @Scheduled(cron = "0 0 3 * * ?", zone = "Europe/Kyiv")
    @SchedulerLock(name = "AuditCleanupJob", lockAtLeastFor = "PT1M", lockAtMostFor = "PT15M")
    @Transactional
    public void purgeExpiredAuditLogs() {
        Instant cutoff = commonGenerator.now().minus(Duration.ofDays(retentionDays));
        int deletedCount = auditLogRepository.deleteByCreatedAtBefore(cutoff);
        log.info("[SCHEDULER] AuditCleanupJob executed: purged {} expired audit log record(s) older than {}",
                deletedCount, cutoff);
    }
}
