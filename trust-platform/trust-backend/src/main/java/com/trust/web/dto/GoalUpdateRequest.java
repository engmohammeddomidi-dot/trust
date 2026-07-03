package com.trust.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GoalUpdateRequest(
        @NotBlank String type,
        @Min(1) @Max(5) int priority
) {}
