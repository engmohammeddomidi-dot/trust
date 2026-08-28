package com.trust.web.dto;

import java.time.LocalDate;

public record SupplierOrderActionDto(
        Long purchaseId,
        String supplierResponse,
        LocalDate supplierRespondedAt,
        LocalDate supplierPromisedDate,
        String supplierRejectionReason
) {}
