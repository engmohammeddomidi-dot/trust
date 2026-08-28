package com.trust.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record StockCountRequest(
        @NotNull Long branchId,
        @NotNull Long itemId,
        String countDate,
        @PositiveOrZero double countedQuantity,
        String note
) {}
