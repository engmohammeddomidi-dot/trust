package com.trust.web.dto;

import java.time.LocalDate;

public record PurchaseDto(
        Long id,
        Long itemId,
        String itemName,
        Long decisionId,
        Long supplierId,
        String supplierName,
        double quantity,
        double costPrice,
        double totalCost,
        LocalDate purchaseDate,
        String status,
        Double receivedQuantity,
        LocalDate receivedDate,
        Boolean priceMatched,
        boolean hasDamage,
        boolean hasDiscrepancy
) {}
