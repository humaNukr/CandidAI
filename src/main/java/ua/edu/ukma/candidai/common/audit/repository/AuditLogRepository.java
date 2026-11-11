package ua.edu.ukma.candidai.common.audit.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.edu.ukma.candidai.common.audit.model.AuditLogEntity;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {

    List<AuditLogEntity> findByActionOrderByCreatedAtDesc(String action);

    List<AuditLogEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<AuditLogEntity> findBySuccessFalseOrderByCreatedAtDesc();
}
