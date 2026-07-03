package com.trust.web.dto;

/**
 * resetToken معروض مباشرة في الاستجابة لأن المنصة لا تملك خدمة بريد/رسائل حقيقية بعد -
 * حل مؤقت ليبقى تدفق استعادة كلمة المرور قابلًا للاستخدام فعليًا. يُحذف هذا الحقل فور
 * ربط خدمة إرسال بريد حقيقية (استبدال LoggingEmailSender بخدمة فعلية).
 */
public record ForgotPasswordResponse(String message, String resetToken) {}
