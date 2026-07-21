package com.trust.web.dto;

public record PerformanceImpactSummaryDto(
        Double performanceScore,
        int risksResolvedCount,
        int opportunitiesResolvedCount,
        double recommendationsCompletionRatePercent
) {}
