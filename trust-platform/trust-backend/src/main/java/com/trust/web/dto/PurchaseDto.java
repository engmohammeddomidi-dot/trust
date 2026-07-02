package com.trust.web.dto;

import java.time.LocalDate;

public record PurchaseDto(
        Long id,
        Long itemId,
        String itemName,
        String supplierName,
        double quantity,
        double costPrice,
        double totalCost,
        LocalDate purchaseDate
) {}
