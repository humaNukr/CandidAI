package ua.edu.ukma.candidai.common.audit.model;

import java.util.UUID;

public interface AuditPrincipal {

    UUID getId();

    String getEmail();
}
