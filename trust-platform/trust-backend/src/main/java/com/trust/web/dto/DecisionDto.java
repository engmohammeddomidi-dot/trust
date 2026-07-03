package com.trust.web.dto;

import java.time.LocalDateTime;

public record DecisionDto(
        Long id,
        Long itemId,
        String itemName,
        Long supplierId,
        String supplierName,
        String type,
        String status,
        double suggestedQuantity,
        Double approvedQuantity,
        String reasonSummary,
        double confidenceScore,
        double financialImpact,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        String actualOutcome
) {}
