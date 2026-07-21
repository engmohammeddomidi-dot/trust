package com.trust.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record SupplierUpdateRequest(
        @NotBlank String name,
        String contactInfo,
        String email,
        @PositiveOrZero int leadTimeDays,
        @PositiveOrZero int creditTermsDays,
        double rating
) {}
