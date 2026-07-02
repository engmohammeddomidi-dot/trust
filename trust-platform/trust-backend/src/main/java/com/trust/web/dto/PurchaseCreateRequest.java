package com.trust.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record PurchaseCreateRequest(
        @NotNull Long branchId,
        Long itemId,
        @NotBlank String supplierName,
        @Positive double quantity,
        @Positive double costPrice,
        @NotNull LocalDate purchaseDate
) {}
