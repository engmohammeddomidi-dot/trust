package com.trust.web;

import com.trust.config.AuthenticatedUser;
import com.trust.config.TenantAccessGuard;
import com.trust.domain.Branch;
import com.trust.service.DashboardService;
import com.trust.service.HealthScoreService;
import com.trust.web.dto.DashboardResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final HealthScoreService healthScoreService;
    private final TenantAccessGuard accessGuard;

    public DashboardController(DashboardService dashboardService, HealthScoreService healthScoreService,
                                TenantAccessGuard accessGuard) {
        this.dashboardService = dashboardService;
        this.healthScoreService = healthScoreService;
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

    /** يحفظ لقطة اليوم من مؤشر صحة الأعمال يدويًا - بديل مبسّط لانتظار الجدولة اليومية الساعة 6 صباحًا */
    @PostMapping("/snapshot")
    public void snapshotToday(@RequestParam Long branchId, @AuthenticationPrincipal AuthenticatedUser principal) {
        Branch branch = accessGuard.requireBranch(principal, branchId);
        healthScoreService.snapshotToday(branch);
    }
}
