package com.trust.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, message = "كلمة المرور يجب أن تكون 8 أحرف على الأقل") String password,
        @NotBlank String role,
        Long branchId
) {}
