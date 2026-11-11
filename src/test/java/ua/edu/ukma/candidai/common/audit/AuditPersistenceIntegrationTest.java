package ua.edu.ukma.candidai.common.audit;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ua.edu.ukma.candidai.common.annotation.IntegrationTest;
import ua.edu.ukma.candidai.common.audit.model.AuditLogEntity;
import ua.edu.ukma.candidai.common.audit.repository.AuditLogRepository;
import ua.edu.ukma.candidai.common.audit.service.AuditLogPersistenceService;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;
import ua.edu.ukma.candidai.vacancy.service.VacancyService;

import static org.assertj.core.api.Assertions.*;
import static ua.edu.ukma.candidai.common.audit.AuditDomainTestResources.*;
import static ua.edu.ukma.candidai.testutils.IntegrationTestResources.*;

@IntegrationTest
class AuditPersistenceIntegrationTest {

    @Autowired
    private AuditLogPersistenceService auditLogPersistenceService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private VacancyService vacancyService;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("saveAuditLog - should persist audit log in PostgreSQL and generate UUID and timestamp")
    void givenValidAuditRecord_saveAuditLog_shouldPersistRowInDatabase() {
        AuditLogEntity saved = auditLogPersistenceService.saveAuditLog(successfulAuditRecord());

        AuditLogEntity persisted = auditLogRepository.findById(saved.getId()).orElseThrow();

        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted)
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt")
                .isEqualTo(expectedSuccessfulAuditEntity());
    }

    @Test
    @DisplayName("saveAuditLog - should commit in REQUIRES_NEW transaction when outer transaction rolls back")
    void givenOuterTransactionRollback_saveAuditLog_shouldKeepAuditRecordCommittedWhileRollingBackBusinessData() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        CreateVacancyRequest vacancyRequest = aValidCreateVacancyRequest();

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            vacancyService.createVacancy(vacancyRequest);
            auditLogPersistenceService.saveAuditLog(failedAuditRecord());
            throw new IllegalStateException("Simulated business failure after audit log");
        })).isInstanceOf(IllegalStateException.class);

        Long vacancyCount = jdbcClient.sql("SELECT COUNT(*) FROM vacancies WHERE title = :title")
                .param("title", vacancyRequest.title())
                .query(Long.class)
                .single();
        List<AuditLogEntity> failedLogs = auditLogRepository.findBySuccessFalseOrderByCreatedAtDesc();

        assertThat(vacancyCount).isZero();
        assertThat(failedLogs)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "createdAt")
                .containsExactly(expectedFailedAuditEntity());
    }

    @Test
    @DisplayName("findByActionAndUserId - should filter audit logs by action and userId ordered by createdAt desc")
    void givenMultipleAuditRecords_queryRepositoryMethods_shouldReturnFilteredRecords() {
        auditLogPersistenceService.saveAuditLog(successfulAuditRecord());
        auditLogPersistenceService.saveAuditLog(candidateApplyAuditRecord());

        List<AuditLogEntity> byAction = auditLogRepository.findByActionOrderByCreatedAtDesc(ACTION_CREATE_VACANCY);
        List<AuditLogEntity> byUser = auditLogRepository.findByUserIdOrderByCreatedAtDesc(USER_ID_CANDIDATE);

        assertThat(byAction)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "createdAt")
                .containsExactly(expectedSuccessfulAuditEntity());
        assertThat(byUser)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "createdAt")
                .containsExactly(expectedCandidateApplyEntity());
    }
}
