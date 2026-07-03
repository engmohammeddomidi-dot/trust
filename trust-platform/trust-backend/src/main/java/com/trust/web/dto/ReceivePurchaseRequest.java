package com.trust.web.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record ReceivePurchaseRequest(
        @PositiveOrZero double receivedQuantity,
        boolean priceMatched,
        boolean hasDamage
) {}
