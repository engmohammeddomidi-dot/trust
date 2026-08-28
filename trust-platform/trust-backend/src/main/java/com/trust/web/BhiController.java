package com.trust.web;

import com.trust.config.AuthenticatedUser;
import com.trust.config.TenantAccessGuard;
import com.trust.domain.Branch;
import com.trust.service.BhiService;
import com.trust.web.dto.BhiResultDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * مؤشر صحة الأعمال بتفصيله الكامل - المحاور الخمسة والمؤشرات الثلاثة عشر مع شرح كل
 * مؤشر وسبب عدم توفّر ما هو غير متوفر. لوحة التحكم تعرض الرقم العام فقط؛ هذا المسار
 * هو التفصيل الذي يجعل الرقم قابلًا للتفسير بدل كونه صندوقًا أسود.
 */
@RestController
@RequestMapping("/api/bhi")
public class BhiController {

    private final BhiService bhiService;
    private final TenantAccessGuard accessGuard;

    public BhiController(BhiService bhiService, TenantAccessGuard accessGuard) {
        this.bhiService = bhiService;
        this.accessGuard = accessGuard;
    }

    @GetMapping
    public BhiResultDto getBhi(
            @RequestParam Long branchId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        Branch branch = accessGuard.requireBranch(principal, branchId);

        LocalDate toDate = to != null ? LocalDate.parse(to) : LocalDate.now();
        LocalDate fromDate = from != null ? LocalDate.parse(from) : toDate.minusDays(30);
        return bhiService.calculate(branch, fromDate, toDate);
    }
}
