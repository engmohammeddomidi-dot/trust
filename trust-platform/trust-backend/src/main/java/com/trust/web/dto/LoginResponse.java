package com.trust.web.dto;

public record LoginResponse(String token, UserSummaryDto user) {}
