package com.trust.web;

import com.trust.config.AuthenticatedUser;
import com.trust.config.TenantAccessGuard;
import com.trust.domain.Branch;
import com.trust.domain.Organization;
import com.trust.domain.User;
import com.trust.repository.OrganizationRepository;
import com.trust.repository.UserRepository;
import com.trust.service.AuditLogService;
import com.trust.web.dto.CreateUserRequest;
import com.trust.web.dto.UserListDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * إدارة مستخدمي المؤسسة (فريق العمل) من قِبل صاحب المؤسسة - القسم 7.11 من خطة MVP.
 * إضافة/تعطيل/تفعيل موظفين ضمن نفس المؤسسة فقط، لا وصول عابر بين المؤسسات.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantAccessGuard accessGuard;
    private final AuditLogService auditLogService;

    public UserController(UserRepository userRepository, OrganizationRepository organizationRepository,
                           PasswordEncoder passwordEncoder, TenantAccessGuard accessGuard, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessGuard = accessGuard;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<UserListDto> list(@AuthenticationPrincipal AuthenticatedUser principal) {
        return userRepository.findByOrganizationId(principal.organizationId()).stream()
                .map(UserController::toDto).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserListDto create(@Valid @RequestBody CreateUserRequest request, @AuthenticationPrincipal AuthenticatedUser principal) {
        requireOwner(principal);

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalStateException("البريد الإلكتروني مستخدم مسبقًا");
        }

        User.Role role;
        try {
            role = User.Role.valueOf(request.role());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("دور غير صالح");
        }
        if (role == User.Role.PLATFORM_ADMIN) {
            throw new AccessDeniedException("لا يمكن إنشاء مستخدم أدمن منصة من هنا");
        }

        Organization org = organizationRepository.findById(principal.organizationId()).orElseThrow();
        Branch branch = request.branchId() != null ? accessGuard.requireBranch(principal, request.branchId()) : null;

        User user = new User();
        user.setOrganization(org);
        user.setBranch(branch);
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setActive(true);

        User saved = userRepository.save(user);
        auditLogService.record(principal.organizationId(), principal.email(), "CREATE_USER", "User", saved.getId().toString(),
                "email=" + saved.getEmail() + ", role=" + saved.getRole());
        return toDto(saved);
    }

    @PatchMapping("/{id}/deactivate")
    public UserListDto deactivate(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal) {
        return setActive(id, false, principal);
    }

    @PatchMapping("/{id}/activate")
    public UserListDto activate(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal) {
        return setActive(id, true, principal);
    }

    private UserListDto setActive(Long id, boolean active, AuthenticatedUser principal) {
        requireOwner(principal);
        User user = userRepository.findById(id).orElseThrow();
        if (user.getOrganization() == null || !user.getOrganization().getId().equals(principal.organizationId())) {
            throw new AccessDeniedException("لا تملك صلاحية إدارة هذا المستخدم");
        }
        if (user.getId().equals(principal.userId()) && !active) {
            throw new IllegalStateException("لا يمكنك تعطيل حسابك الخاص");
        }
        user.setActive(active);
        User saved = userRepository.save(user);
        auditLogService.record(principal.organizationId(), principal.email(),
                active ? "ACTIVATE_USER" : "DEACTIVATE_USER", "User", saved.getId().toString(), null);
        return toDto(saved);
    }

    private void requireOwner(AuthenticatedUser principal) {
        if (!"OWNER".equals(principal.role())) {
            throw new AccessDeniedException("فقط صاحب المؤسسة يمكنه إدارة المستخدمين");
        }
    }

    private static UserListDto toDto(User user) {
        return new UserListDto(
                user.getId(), user.getName(), user.getEmail(), user.getRole().name(),
                user.getBranch() != null ? user.getBranch().getId() : null,
                user.getBranch() != null ? user.getBranch().getName() : null,
                user.isActive()
        );
    }
}
