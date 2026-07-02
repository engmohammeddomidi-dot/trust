package com.trust.web;

import com.trust.config.AuthenticatedUser;
import com.trust.config.JwtService;
import com.trust.domain.User;
import com.trust.repository.UserRepository;
import com.trust.web.dto.LoginRequest;
import com.trust.web.dto.LoginResponse;
import com.trust.web.dto.UserSummaryDto;
import jakarta.validation.Valid;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("بيانات الدخول غير صحيحة"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("بيانات الدخول غير صحيحة");
        }

        String token = jwtService.generateToken(user);
        return new LoginResponse(token, toDto(user));
    }

    @GetMapping("/me")
    public UserSummaryDto me(@AuthenticationPrincipal AuthenticatedUser principal) {
        User user = userRepository.findById(principal.userId()).orElseThrow();
        return toDto(user);
    }

    private static UserSummaryDto toDto(User user) {
        return new UserSummaryDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getOrganization() != null ? user.getOrganization().getId() : null,
                user.getOrganization() != null ? user.getOrganization().getName() : null,
                user.getBranch() != null ? user.getBranch().getId() : null
        );
    }
}
