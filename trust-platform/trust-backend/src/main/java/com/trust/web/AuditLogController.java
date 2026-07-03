package com.trust.web;

import com.trust.config.AuthenticatedUser;
import com.trust.domain.AuditLog;
import com.trust.repository.AuditLogRepository;
import com.trust.web.dto.AuditLogDto;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** سجل تدقيق العمليات الحساسة لهذه المؤسسة - لصاحب المؤسسة فقط */
@RestController
@RequestMapping("/api/audit-log")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public List<AuditLogDto> list(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (!"OWNER".equals(principal.role())) {
            throw new AccessDeniedException("فقط صاحب المؤسسة يمكنه مراجعة سجل التدقيق");
        }
        return auditLogRepository.findByOrganizationIdOrderByCreatedAtDesc(principal.organizationId())
                .stream().map(AuditLogController::toDto).toList();
    }

    private static AuditLogDto toDto(AuditLog log) {
        return new AuditLogDto(log.getId(), log.getActorEmail(), log.getAction(), log.getEntityType(),
                log.getEntityId(), log.getDetails(), log.getCreatedAt());
    }
}
