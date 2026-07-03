package com.trust.web.dto;

public record LoginResponse(String token, String refreshToken, UserSummaryDto user) {}
