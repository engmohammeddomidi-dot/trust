package com.trust.web.dto;

public record CategoryBenchmarkDto(
        String category,
        double targetMarginPercent,
        double liquidityRatioMin,
        double liquidityRatioMax,
        double inventoryCoverageMinMonths,
        double inventoryCoverageMaxMonths,
        int stagnationDaysThreshold,
        int slowMovingDaysThreshold,
        int mediumMovingDaysThreshold,
        double weightSales,
        double weightProfit,
        double weightPricing,
        double weightPurchases,
        double weightInventory,
        double weightLiquidity
) {}
