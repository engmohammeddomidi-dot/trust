package com.trust.web.dto;

public record HealthScoreDto(
        double salesScore,
        double profitScore,
        double pricingScore,
        double purchasesScore,
        double inventoryScore,
        double liquidityScore,
        double totalScore,
        String label // ممتاز / جيد / مقبول / ضعيف
) {}
