package com.trust.web.dto;

import java.util.List;

public record MonthlyImpactLedgerDto(
        double purchaseCostSavings,
        double inventoryRiskImpact,
        double operatingProfitImpact,
        double totalFinancialImpact,
        List<TrendPoint> performanceTrend
) {
    public record TrendPoint(String date, double score) {}
}
