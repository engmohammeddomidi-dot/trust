package com.trust.web.dto;

public record UserSummaryDto(
        Long id,
        String name,
        String email,
        String role,
        Long organizationId,
        String organizationName,
        Long branchId,
        boolean tosAccepted
) {}
