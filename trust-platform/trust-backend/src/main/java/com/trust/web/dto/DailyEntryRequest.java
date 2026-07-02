package com.trust.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

public record DailyEntryRequest(
        @NotNull Long branchId,
        @NotNull LocalDate entryDate,
        @PositiveOrZero double totalSales,
        @PositiveOrZero double totalCogs,
        Double totalProfit, // optional - computed if null (totalSales - totalCogs)
        @PositiveOrZero double availableLiquidity,
        @PositiveOrZero double receivables,
        @PositiveOrZero double payables
) {}
