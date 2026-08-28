package com.trust.web.dto;

import java.time.LocalDate;

public record SupplierPortalPurchaseDto(
        Long id,
        String organizationName,
        String branchName,
        String itemName,
        double quantity,
        double costPrice,
        String status,
        LocalDate purchaseDate,
        LocalDate expectedDeliveryDate,
        LocalDate receivedDate,
        String supplierResponse,
        LocalDate supplierPromisedDate,
        String supplierRejectionReason
) {}
