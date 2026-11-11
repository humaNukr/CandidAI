package ua.edu.ukma.candidai.common.audit.service;

import ua.edu.ukma.candidai.common.audit.model.AuditLogRecord;

public interface AuditLogPersistenceService {

    void saveAuditLog(AuditLogRecord record);
}
