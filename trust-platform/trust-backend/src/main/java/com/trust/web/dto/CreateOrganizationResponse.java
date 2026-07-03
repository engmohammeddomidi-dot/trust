package com.trust.web.dto;

public record CreateOrganizationResponse(
        Long organizationId,
        String organizationName,
        Long branchId,
        String ownerEmail,
        String temporaryPassword
) {}
