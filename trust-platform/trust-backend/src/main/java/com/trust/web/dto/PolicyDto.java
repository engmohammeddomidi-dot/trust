package com.trust.web.dto;

public record PolicyDto(
        double maxPurchaseLiquidityRatio,
        double minSupplierRating
) {}
