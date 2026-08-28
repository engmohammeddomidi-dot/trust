package com.trust.web.dto;

public record WasteRecordDto(
        Long id,
        Long itemId,
        String itemName,
        String wasteDate,
        double quantity,
        double unitCost,
        double totalCost,
        String reason,
        String note
) {}
