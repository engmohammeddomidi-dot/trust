package com.trust.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BulkImportItemsRequest(@NotNull Long branchId, @NotEmpty List<ItemImportRow> items) {}
