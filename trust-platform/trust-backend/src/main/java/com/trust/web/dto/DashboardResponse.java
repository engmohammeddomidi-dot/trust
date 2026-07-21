package com.trust.web.dto;

import java.util.List;
import java.util.Map;

public record DashboardResponse(
        double salesToday,
        double salesChangePercent,
        double totalProfit,
        double profitChangePercent,
        double marginPercent,
        double marginChangePercent,
        double availableLiquidity,
        double liquidityChangePercent,
        HealthScoreDto healthScore,
        List<SalesPoint> salesTrend,
        List<RecommendationDto> topRecommendations,
        Map<String, Double> inventoryBreakdown, // FAST/MEDIUM/SLOW/STAGNANT -> value
        Map<String, Double> liquidityBreakdown, // AVAILABLE/RECEIVABLES/PAYABLES
        List<ItemDto> itemsNeedingAttention,
        DailyPerformanceSummaryDto dailyPerformanceSummary,
        PerformanceImpactSummaryDto performanceImpactSummary,
        MonthlyImpactLedgerDto monthlyImpactLedger,
        ExecutiveActionCenterDto executiveActionCenter
) {
    public record SalesPoint(String date, double sales) {}
}
