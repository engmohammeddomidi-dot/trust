package com.trust.config;

/** يمثّل هوية المستخدم المستخرجة من الـ JWT، متاح داخل الطلب عبر SecurityContext */
public record AuthenticatedUser(Long userId, String email, String role, Long organizationId, Long branchId) {}
