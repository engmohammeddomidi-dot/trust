package com.trust.web.dto;

import java.time.LocalDateTime;

public record AdminDecisionDto(
        Long id,
        String organizationName,
        String branchName,
        String itemName,
        String category,
        double financialImpact,
        double confidenceScore,
        String reasonSummary,
        LocalDateTime createdAt
) {}
