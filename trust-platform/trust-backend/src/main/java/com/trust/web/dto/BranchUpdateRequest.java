package com.trust.web.dto;

import jakarta.validation.constraints.NotBlank;

public record BranchUpdateRequest(@NotBlank String name, String city, boolean active) {}
