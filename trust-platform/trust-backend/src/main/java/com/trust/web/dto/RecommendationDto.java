package com.trust.web.dto;

public record RecommendationDto(
        Long id,
        String type,
        String priority,
        String title,
        double expectedValue,
        String status
) {}
