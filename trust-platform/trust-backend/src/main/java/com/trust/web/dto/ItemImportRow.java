package com.trust.web.dto;

import java.time.LocalDate;

public record ItemImportRow(
        String name,
        String subCategory,
        double costPrice,
        double salePrice,
        double quantity,
        LocalDate lastSaleDate,
        LocalDate expiryDate
) {}
