package com.trust.web.dto;

import java.time.LocalDate;

public record ItemDto(
        Long id,
        String name,
        String subCategory,
        double costPrice,
        double salePrice,
        double marginPercent,
        double quantity,
        double inventoryValue,
        LocalDate lastSaleDate,
        LocalDate expiryDate,
        String movementStatus,
        Long supplierId,
        String supplierName,
        int safetyStockDays
) {}
