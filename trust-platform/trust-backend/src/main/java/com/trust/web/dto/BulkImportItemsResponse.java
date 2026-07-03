package com.trust.web.dto;

import java.util.List;

public record BulkImportItemsResponse(int createdCount, List<String> errors) {}
