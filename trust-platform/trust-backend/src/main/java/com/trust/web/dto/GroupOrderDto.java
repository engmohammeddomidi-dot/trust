package com.trust.web.dto;

import java.time.LocalDateTime;

public record GroupOrderDto(
        Long id,
        String itemName,
        double targetQuantity,
        double currentQuantity,
        double estimatedMarketPrice,
        Double negotiatedPrice,
        String status,
        int participantCount,
        LocalDateTime createdAt
) {}
