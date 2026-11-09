package ua.edu.ukma.candidai.common.audit.service;

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.candidai.common.audit.model.AuditLogEntity;
import ua.edu.ukma.candidai.common.audit.model.AuditLogRecord;
import ua.edu.ukma.candidai.common.audit.repository.AuditLogRepository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static ua.edu.ukma.candidai.common.audit.AuditDomainTestResources.*;

@ExtendWith(MockitoExtension.class)
class AuditLogPersistenceServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogPersistenceService auditLogPersistenceService;

    @Test
    @DisplayName("saveAuditLog - should convert record to entity and persist via repository")
    void givenValidAuditRecord_saveAuditLog_shouldPersistMappedEntity() {
        AuditLogRecord record = successfulAuditRecord();
        AuditLogEntity expected = expectedSuccessfulAuditEntity();

        when(auditLogRepository.save(any(AuditLogEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditLogEntity actual = auditLogPersistenceService.saveAuditLog(record);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("saveAuditLog - should preserve failure metadata when saving failed operation record")
    void givenFailedAuditRecord_saveAuditLog_shouldPersistFailureDetails() {
        AuditLogRecord record = failedAuditRecord();
        AuditLogEntity expected = expectedFailedAuditEntity();
        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);

        AuditLogPersistenceService service = new AuditLogPersistenceService(auditLogRepository);
        service.saveAuditLog(record);

        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("saveAuditLog - should declare REQUIRES_NEW transaction propagation")
    void givenSaveAuditLogMethod_inspectTransactionalAnnotation_shouldUseRequiresNewPropagation()
            throws NoSuchMethodException {
        Method method = AuditLogPersistenceService.class.getMethod("saveAuditLog", AuditLogRecord.class);

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }
}
