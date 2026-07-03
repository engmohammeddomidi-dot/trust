package com.trust.web.dto;

import jakarta.validation.constraints.Positive;

public record DecisionModifyRequest(
        @Positive double quantity,
        Long supplierId
) {}
