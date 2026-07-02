package com.trust.web.dto;

public record BranchDto(Long id, Long organizationId, String name, String city, boolean active) {}
