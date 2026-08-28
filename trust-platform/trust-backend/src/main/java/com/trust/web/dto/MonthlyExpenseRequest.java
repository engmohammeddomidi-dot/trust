package com.trust.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record MonthlyExpenseRequest(
        @NotNull Long branchId,
        @NotNull String month,
        @NotNull String category,
        @PositiveOrZero double unitAmount,
        @Min(1) int quantity,
        String note
) {}
