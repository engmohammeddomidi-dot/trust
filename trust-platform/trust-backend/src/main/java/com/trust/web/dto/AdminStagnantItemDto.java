package com.trust.web.dto;

import java.time.LocalDate;

public record AdminStagnantItemDto(
        String organizationName,
        String branchName,
        String itemName,
        double quantity,
        double inventoryValue,
        LocalDate lastSaleDate
) {}
