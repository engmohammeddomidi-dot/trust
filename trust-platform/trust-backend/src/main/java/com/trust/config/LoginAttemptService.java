package com.trust.config;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * حماية بسيطة في الذاكرة ضد محاولات تخمين كلمة المرور (Brute-force) - القسم الأمني
 * من مراجعة "market grade". يكفي لخادم واحد؛ نشر متعدد النسخ سيحتاج مخزنًا مشتركًا (Redis).
 */
@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_SECONDS = 60;

    private record Attempts(AtomicInteger count, Instant windowStart) {}

    private final ConcurrentHashMap<String, Attempts> attemptsByEmail = new ConcurrentHashMap<>();

    public void recordFailure(String email) {
        attemptsByEmail.compute(email.toLowerCase(), (key, existing) -> {
            if (existing == null || Instant.now().isAfter(existing.windowStart().plusSeconds(LOCKOUT_SECONDS))) {
                return new Attempts(new AtomicInteger(1), Instant.now());
            }
            existing.count().incrementAndGet();
            return existing;
        });
    }

    public void recordSuccess(String email) {
        attemptsByEmail.remove(email.toLowerCase());
    }

    public boolean isLocked(String email) {
        Attempts attempts = attemptsByEmail.get(email.toLowerCase());
        if (attempts == null) return false;
        if (Instant.now().isAfter(attempts.windowStart().plusSeconds(LOCKOUT_SECONDS))) {
            attemptsByEmail.remove(email.toLowerCase());
            return false;
        }
        return attempts.count().get() >= MAX_ATTEMPTS;
    }
}
