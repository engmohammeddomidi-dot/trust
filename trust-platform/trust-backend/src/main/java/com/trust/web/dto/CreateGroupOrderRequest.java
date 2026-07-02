package com.trust.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateGroupOrderRequest(
        @NotBlank String itemName,
        @Positive double targetQuantity,
        @Positive double estimatedMarketPrice
) {}
