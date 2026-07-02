package com.trust.web.dto;

import jakarta.validation.constraints.Positive;

public record JoinGroupOrderRequest(@Positive double quantity) {}
