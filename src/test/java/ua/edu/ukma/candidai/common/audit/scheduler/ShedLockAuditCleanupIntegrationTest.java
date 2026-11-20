package ua.edu.ukma.candidai.common.audit.scheduler;

import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.spring.api.DBRider;
import java.util.List;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ua.edu.ukma.candidai.common.annotation.IntegrationTest;
import ua.edu.ukma.candidai.common.audit.model.AuditLogEntity;
import ua.edu.ukma.candidai.common.audit.repository.AuditLogRepository;

import static org.assertj.core.api.Assertions.*;
import static ua.edu.ukma.candidai.common.audit.AuditDomainTestResources.*;

@IntegrationTest
@DBRider
@DBUnit(caseSensitiveTableNames = true, cacheConnection = false)
class ShedLockAuditCleanupIntegrationTest {

    @Autowired
    private AuditCleanupScheduler auditCleanupScheduler;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private LockProvider lockProvider;

    @Test
    @DisplayName("purgeExpiredAuditLogs - should acquire ShedLock in database and remove expired audit logs")
    @DataSet(value = "datasets/audit/expired-and-recent-audit-logs.yml", cleanBefore = true)
    @ExpectedDataSet(
            value = "datasets/audit/expected-after-audit-cleanup.yml",
            ignoreCols = {"created_at", "lock_until", "locked_at"}
    )
    void givenExpiredAndRecentAuditLogs_purgeExpiredAuditLogs_shouldAcquireShedLockAndDeleteOnlyExpiredRecords() {
        auditCleanupScheduler.purgeExpiredAuditLogs();

        List<AuditLogEntity> remainingLogs = auditLogRepository.findAll();

        assertThat(lockProvider).isNotNull();
        assertThat(remainingLogs)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "createdAt")
                .containsExactly(expectedCandidateApplyEntity());
    }

    @Test
    @DisplayName("purgeExpiredAuditLogs - should skip execution when ShedLock is already held by another instance")
    @DataSet(value = "datasets/audit/active-lock-by-another-node.yml", cleanBefore = true)
    @ExpectedDataSet(
            value = "datasets/audit/expected-when-locked-by-another-node.yml",
            ignoreCols = {"created_at", "lock_until", "locked_at"}
    )
    void givenActiveShedLockHeldByAnotherNode_purgeExpiredAuditLogs_shouldSkipCleanupExecution() {
        auditCleanupScheduler.purgeExpiredAuditLogs();

        List<AuditLogEntity> remainingLogs = auditLogRepository.findAll();

        assertThat(remainingLogs)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "createdAt")
                .containsExactly(expectedSuccessfulAuditEntity());
    }
}
