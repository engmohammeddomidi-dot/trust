package com.trust.web.dto;

public record UserListDto(
        Long id,
        String name,
        String email,
        String role,
        Long branchId,
        String branchName,
        boolean active
) {}
