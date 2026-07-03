package com.trust.web;

import com.trust.config.AuthenticatedUser;
import com.trust.config.TenantAccessGuard;
import com.trust.domain.Notification;
import com.trust.repository.NotificationRepository;
import com.trust.web.dto.NotificationDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** مركز تنبيهات داخل التطبيق - بديل عن البريد/واتساب لحين توفّر خدمة إرسال حقيقية */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final TenantAccessGuard accessGuard;

    public NotificationController(NotificationRepository notificationRepository, TenantAccessGuard accessGuard) {
        this.notificationRepository = notificationRepository;
        this.accessGuard = accessGuard;
    }

    @GetMapping
    public List<NotificationDto> list(@AuthenticationPrincipal AuthenticatedUser principal) {
        return notificationRepository.findByOrganizationIdOrderByCreatedAtDesc(principal.organizationId())
                .stream().map(NotificationController::toDto).toList();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal AuthenticatedUser principal) {
        return Map.of("count", notificationRepository.countByOrganizationIdAndReadAtIsNull(principal.organizationId()));
    }

    @PatchMapping("/{id}/read")
    public NotificationDto markRead(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal) {
        Notification n = notificationRepository.findById(id).orElseThrow();
        if (!n.getOrganizationId().equals(principal.organizationId())) {
            accessGuard.requireOrganization(principal, n.getOrganizationId());
        }
        n.setReadAt(LocalDateTime.now());
        return toDto(notificationRepository.save(n));
    }

    private static NotificationDto toDto(Notification n) {
        return new NotificationDto(n.getId(), n.getTitle(), n.getMessage(), n.getSeverity().name(), n.getCreatedAt(), n.getReadAt());
    }
}
