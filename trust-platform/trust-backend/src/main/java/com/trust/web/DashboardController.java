package com.trust.web;

import com.trust.config.AuthenticatedUser;
import com.trust.config.TenantAccessGuard;
import com.trust.service.DashboardService;
import com.trust.web.dto.DashboardResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final TenantAccessGuard accessGuard;

    public DashboardController(DashboardService dashboardService, TenantAccessGuard accessGuard) {
        this.dashboardService = dashboardService;
        this.accessGuard = accessGuard;
    }

    @GetMapping
    public DashboardResponse getDashboard(
            @RequestParam Long organizationId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireOrganization(principal, organizationId);
        if (branchId != null) accessGuard.requireBranch(principal, branchId);

        LocalDate toDate = to != null ? LocalDate.parse(to) : LocalDate.now();
        LocalDate fromDate = from != null ? LocalDate.parse(from) : toDate.minusDays(6);
        return dashboardService.build(organizationId, branchId, fromDate, toDate);
    }
}
