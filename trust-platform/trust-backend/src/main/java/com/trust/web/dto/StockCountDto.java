package com.trust.web.dto;

public record StockCountDto(
        Long id,
        Long itemId,
        String itemName,
        String countDate,
        double expectedQuantity,
        double countedQuantity,
        double discrepancy,
        String note
) {}
