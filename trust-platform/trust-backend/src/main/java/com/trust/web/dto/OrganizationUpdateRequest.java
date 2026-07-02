package com.trust.web.dto;

import jakarta.validation.constraints.NotBlank;

public record OrganizationUpdateRequest(@NotBlank String name) {}
