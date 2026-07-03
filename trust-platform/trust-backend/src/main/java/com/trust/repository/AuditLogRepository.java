package com.trust.repository;

import com.trust.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);
}
