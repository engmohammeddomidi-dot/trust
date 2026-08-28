package com.trust.web.dto;

import com.trust.domain.Category;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record BhiWeightUpdateRequest(
        @NotNull Category category,
        @NotNull String axis,
        @PositiveOrZero double weight
) {}
