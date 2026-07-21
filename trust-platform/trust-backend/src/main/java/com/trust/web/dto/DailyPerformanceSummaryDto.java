package com.trust.web.dto;

public record DailyPerformanceSummaryDto(
        double groupBuySavingsRatePercent,
        double groupBuySavingsAmountThisMonth,
        double inventoryTurnoverRatePercent,
        double purchaseVolumeNeeded,
        double clearanceVolumeNeeded
) {}
