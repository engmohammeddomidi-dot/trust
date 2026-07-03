package com.trust.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SupplierCreateRequest(
        @NotNull Long organizationId,
        @NotBlank String name,
        String contactInfo,
        @PositiveOrZero int leadTimeDays,
        @PositiveOrZero int creditTermsDays,
        double rating
) {}
