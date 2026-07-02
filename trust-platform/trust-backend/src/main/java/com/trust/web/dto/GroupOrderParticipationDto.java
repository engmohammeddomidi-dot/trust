package com.trust.web.dto;

public record GroupOrderParticipationDto(
        Long groupOrderId,
        String itemName,
        double quantity,
        String status,
        double estimatedMarketPrice,
        Double negotiatedPrice,
        Double savings
) {}
