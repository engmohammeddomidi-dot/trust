package com.trust.web.dto;

import jakarta.validation.constraints.NotBlank;

/** حقوق الملكية اختيارية - تركها فارغة يُبقي مؤشر نسبة الدين "غير متاح" بصدق */
public record OrganizationUpdateRequest(@NotBlank String name, Double equity) {}
