package com.trust.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record ItemCreateRequest(
        @NotNull Long branchId,
        @NotBlank String name,
        String subCategory,
        @Positive double costPrice,
        @Positive double salePrice,
        @Positive double quantity,
        LocalDate lastSaleDate,
        LocalDate expiryDate
) {}
