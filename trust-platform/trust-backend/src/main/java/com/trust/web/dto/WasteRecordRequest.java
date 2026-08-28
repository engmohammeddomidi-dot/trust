package com.trust.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record WasteRecordRequest(
        @NotNull Long branchId,
        @NotNull Long itemId,
        String wasteDate,
        @Positive double quantity,
        @NotNull String reason,
        String note
) {}
