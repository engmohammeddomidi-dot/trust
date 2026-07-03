package com.trust.web.dto;

public record DecisionQualityScoreDto(
        int ordersIssued,
        int ordersReceived,
        int ordersWithDiscrepancy,
        Double qualityScorePercent
) {}
