package com.trust.web.dto;

public record SupplierDto(
        Long id,
        String name,
        String contactInfo,
        int leadTimeDays,
        int creditTermsDays,
        double rating
) {}
