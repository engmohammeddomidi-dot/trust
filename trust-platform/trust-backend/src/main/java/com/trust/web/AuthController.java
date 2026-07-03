package com.trust.web;

import com.trust.config.AuthenticatedUser;
import com.trust.config.JwtService;
import com.trust.config.LoginAttemptService;
import com.trust.config.TooManyAttemptsException;
import com.trust.domain.PasswordResetToken;
import com.trust.domain.RefreshToken;
import com.trust.domain.User;
import com.trust.repository.PasswordResetTokenRepository;
import com.trust.repository.RefreshTokenRepository;
import com.trust.repository.UserRepository;
import com.trust.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final long refreshExpirationMs;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                           LoginAttemptService loginAttemptService, RefreshTokenRepository refreshTokenRepository,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        if (loginAttemptService.isLocked(request.email())) {
            throw new TooManyAttemptsException("محاولات دخول كثيرة فاشلة - حاول مرة أخرى خلال دقيقة");
        }

        // نُسجّل فشل المحاولة سواء كان البريد غير موجود أو كانت كلمة المرور خاطئة، برسالة خطأ
        // موحّدة في الحالتين - وإلا يمكن تجاوز حد المحاولات كليًا بتجربة بريد غير مسجَّل
        User user = userRepository.findByEmail(request.email())
                .orElseGet(() -> {
                    loginAttemptService.recordFailure(request.email());
                    throw new BadCredentialsException("بيانات الدخول غير صحيحة");
                });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptService.recordFailure(request.email());
            throw new BadCredentialsException("بيانات الدخول غير صحيحة");
        }

        if (!user.isActive()) {
            throw new BadCredentialsException("تم تعطيل هذا الحساب - راجع صاحب المؤسسة");
        }

        loginAttemptService.recordSuccess(request.email());
        String accessToken = jwtService.generateToken(user);
        String refreshToken = issueRefreshToken(user);
        return new LoginResponse(accessToken, refreshToken, toDto(user));
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@RequestBody Map<String, String> body) {
        String tokenValue = body.get("refreshToken");
        if (tokenValue == null) throw new BadCredentialsException("رمز التجديد مفقود");

        RefreshToken stored = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new BadCredentialsException("رمز التجديد غير صالح"));
        if (!stored.isValid()) {
            throw new BadCredentialsException("رمز التجديد منتهي الصلاحية - سجّل الدخول مجددًا");
        }

        User user = stored.getUser();
        if (!user.isActive()) {
            throw new BadCredentialsException("تم تعطيل هذا الحساب");
        }

        // تدوير الرمز: نُبطل القديم ونُصدر واحدًا جديدًا في كل تجديد
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        String accessToken = jwtService.generateToken(user);
        String newRefreshToken = issueRefreshToken(user);
        return new LoginResponse(accessToken, newRefreshToken, toDto(user));
    }

    @PostMapping("/logout")
    public void logout(@RequestBody Map<String, String> body) {
        String tokenValue = body.get("refreshToken");
        if (tokenValue == null) return;
        refreshTokenRepository.findByToken(tokenValue).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        });
    }

    /**
     * لا توجد خدمة بريد حقيقية بعد - resetToken يُعاد مباشرة في الاستجابة كحل مؤقت
     * ليبقى تدفق الاستعادة قابلًا للاستخدام. لا يُفصح عن وجود البريد من عدمه في الرسالة.
     */
    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        var userOpt = userRepository.findByEmail(request.email());
        if (userOpt.isEmpty()) {
            return new ForgotPasswordResponse("إذا كان البريد مسجّلًا لدينا، ستصلك تعليمات الاستعادة", null);
        }

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(userOpt.get());
        resetToken.setToken(randomToken());
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        passwordResetTokenRepository.save(resetToken);

        return new ForgotPasswordResponse("إذا كان البريد مسجّلًا لدينا، ستصلك تعليمات الاستعادة", resetToken.getToken());
    }

    @PostMapping("/reset-password")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new BadCredentialsException("رمز الاستعادة غير صالح"));
        if (!resetToken.isValid()) {
            throw new BadCredentialsException("رمز الاستعادة منتهي الصلاحية");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    @PatchMapping("/accept-tos")
    public UserSummaryDto acceptTos(@AuthenticationPrincipal AuthenticatedUser principal) {
        User user = userRepository.findById(principal.userId()).orElseThrow();
        user.setTosAcceptedAt(LocalDateTime.now());
        return toDto(userRepository.save(user));
    }

    @GetMapping("/me")
    public UserSummaryDto me(@AuthenticationPrincipal AuthenticatedUser principal) {
        User user = userRepository.findById(principal.userId()).orElseThrow();
        return toDto(user);
    }

    private String issueRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(randomToken());
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000));
        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static UserSummaryDto toDto(User user) {
        return new UserSummaryDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getOrganization() != null ? user.getOrganization().getId() : null,
                user.getOrganization() != null ? user.getOrganization().getName() : null,
                user.getBranch() != null ? user.getBranch().getId() : null,
                user.getTosAcceptedAt() != null
        );
    }
}
