package com.trust.web.dto;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        String title,
        String message,
        String severity,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {}
