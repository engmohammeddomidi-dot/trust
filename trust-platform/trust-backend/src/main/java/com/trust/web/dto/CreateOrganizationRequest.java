package com.trust.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateOrganizationRequest(
        @NotBlank String organizationName,
        @NotBlank String category,
        @NotBlank String branchName,
        String branchCity,
        @NotBlank String ownerName,
        @Email @NotBlank String ownerEmail
) {}
