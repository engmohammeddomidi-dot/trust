package com.trust.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, message = "كلمة المرور يجب أن تكون 8 أحرف على الأقل") String newPassword
) {}
