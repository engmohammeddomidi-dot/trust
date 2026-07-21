package com.trust.web.dto;

import java.util.List;

public record ExecutiveActionCenterDto(
        List<TopItemDto> topProfitabilityItems,
        List<TopItemDto> topAccumulatedCostItems,
        List<ExecutiveAlertDto> alerts
) {
    public record TopItemDto(String itemName, double value) {}
    public record ExecutiveAlertDto(String type, String label, int count) {}
}
