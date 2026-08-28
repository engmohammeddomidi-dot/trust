package com.trust.web.dto;

import java.util.List;

public record ExecutiveActionCenterDto(
        List<TopItemDto> topProfitabilityItems,
        List<TopItemDto> topAccumulatedCostItems,
        List<ExecutiveAlertDto> alerts,
        /**
         * طابور "فرص اليوم" الموحَّد - مرتَّب بالأثر المتوقَّع ومحدود بخمسة عناصر.
         * يدمج ما كان موزَّعًا على ثلاث شاشات: قرارات الشراء، الأصناف الراكدة،
         * وقرب انتهاء الصلاحية.
         */
        List<OpportunitySignalDto> todaysOpportunities
) {
    public record TopItemDto(String itemName, double value) {}
    public record ExecutiveAlertDto(String type, String label, int count) {}

    public record OpportunitySignalDto(
            String kind,
            String title,
            String detail,
            double expectedImpact,
            String suggestedAction,
            Long itemId
    ) {}
}
