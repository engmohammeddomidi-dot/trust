package com.trust.web.dto;

import jakarta.validation.constraints.Positive;

public record NegotiateGroupOrderRequest(@Positive double negotiatedPrice) {}
