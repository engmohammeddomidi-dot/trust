package com.trust.service;

import com.trust.domain.AuditLog;
import com.trust.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(Long organizationId, String actorEmail, String action, String entityType, String entityId, String details) {
        AuditLog log = new AuditLog();
        log.setOrganizationId(organizationId);
        log.setActorEmail(actorEmail);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        auditLogRepository.save(log);
    }
}
