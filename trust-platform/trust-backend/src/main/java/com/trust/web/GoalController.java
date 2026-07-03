package com.trust.web;

import com.trust.config.AuthenticatedUser;
import com.trust.config.TenantAccessGuard;
import com.trust.domain.Goal;
import com.trust.service.GoalService;
import com.trust.web.dto.GoalDto;
import com.trust.web.dto.GoalUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;
    private final TenantAccessGuard accessGuard;

    public GoalController(GoalService goalService, TenantAccessGuard accessGuard) {
        this.goalService = goalService;
        this.accessGuard = accessGuard;
    }

    /** يعيد أولويات كل الأهداف السبعة للمؤسسة (3 = محايدة لأي هدف لم يُخصَّص بعد) */
    @GetMapping
    public List<GoalDto> list(@RequestParam Long organizationId, @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireOrganization(principal, organizationId);
        return goalService.resolveForOrganization(organizationId).entrySet().stream()
                .map(e -> new GoalDto(e.getKey().name(), e.getValue()))
                .toList();
    }

    @PutMapping
    public List<GoalDto> update(@RequestParam Long organizationId, @Valid @RequestBody List<GoalUpdateRequest> requests,
                                 @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireOrganization(principal, organizationId);
        Map<Goal.Type, Integer> priorities = new EnumMap<>(Goal.Type.class);
        for (GoalUpdateRequest req : requests) {
            Goal.Type type;
            try {
                type = Goal.Type.valueOf(req.type());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("نوع هدف غير معروف: " + req.type());
            }
            priorities.put(type, req.priority());
        }
        return goalService.updateAll(organizationId, priorities).entrySet().stream()
                .map(e -> new GoalDto(e.getKey().name(), e.getValue()))
                .toList();
    }
}
