package com.trust.web.dto;

public record MonthlyExpenseDto(
        Long id,
        String month,
        String category,
        String categoryLabelAr,
        double unitAmount,
        int quantity,
        double total,
        String note
) {}
