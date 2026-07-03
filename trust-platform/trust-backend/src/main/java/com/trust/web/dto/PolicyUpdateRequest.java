package com.trust.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record PolicyUpdateRequest(
        @DecimalMin("0.01") @DecimalMax("1.0") double maxPurchaseLiquidityRatio,
        @DecimalMin("0.0") @DecimalMax("100.0") double minSupplierRating
) {}
