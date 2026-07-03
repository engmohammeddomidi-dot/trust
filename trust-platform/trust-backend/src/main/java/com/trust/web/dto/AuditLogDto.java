package com.trust.web.dto;

import java.time.LocalDateTime;

public record AuditLogDto(
        Long id,
        String actorEmail,
        String action,
        String entityType,
        String entityId,
        String details,
        LocalDateTime createdAt
) {}
