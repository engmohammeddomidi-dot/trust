package com.trust.web.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateCategoryBenchmarkRequest(
        @Positive double targetMarginPercent,
        @PositiveOrZero double liquidityRatioMin,
        @Positive double liquidityRatioMax,
        @Positive double inventoryCoverageMinMonths,
        @Positive double inventoryCoverageMaxMonths,
        @Positive int stagnationDaysThreshold,
        @Positive int slowMovingDaysThreshold,
        @Positive int mediumMovingDaysThreshold,
        @PositiveOrZero double weightSales,
        @PositiveOrZero double weightProfit,
        @PositiveOrZero double weightPricing,
        @PositiveOrZero double weightPurchases,
        @PositiveOrZero double weightInventory,
        @PositiveOrZero double weightLiquidity
) {}
