package com.trust.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ItemLinkSupplierRequest(
        @NotNull Long supplierId,
        @PositiveOrZero Integer safetyStockDays
) {}
