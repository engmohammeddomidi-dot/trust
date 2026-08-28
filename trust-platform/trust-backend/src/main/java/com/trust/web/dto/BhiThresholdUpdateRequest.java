package com.trust.web.dto;

import com.trust.domain.Category;
import jakarta.validation.constraints.NotNull;

public record BhiThresholdUpdateRequest(
        @NotNull Category category,
        @NotNull String code,
        double weak,
        double medium,
        double excellent
) {}
