package ua.edu.ukma.candidai.common.audit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.candidai.common.audit.model.AuditLogEntity;
import ua.edu.ukma.candidai.common.audit.model.AuditLogRecord;
import ua.edu.ukma.candidai.common.audit.repository.AuditLogRepository;

@Service
@RequiredArgsConstructor
public class AuditLogPersistenceService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLogEntity saveAuditLog(AuditLogRecord record) {
        AuditLogEntity entity = record.toEntity();
        return auditLogRepository.save(entity);
    }
}
