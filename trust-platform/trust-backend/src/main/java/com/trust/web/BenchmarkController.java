package com.trust.web;

import com.trust.config.AuthenticatedUser;
import com.trust.config.TenantAccessGuard;
import com.trust.domain.Branch;
import com.trust.domain.CategoryBenchmark;
import com.trust.service.RecommendationEngineService;
import com.trust.web.dto.BenchmarkDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/benchmark")
public class BenchmarkController {

    private final RecommendationEngineService engineService;
    private final TenantAccessGuard accessGuard;

    public BenchmarkController(RecommendationEngineService engineService, TenantAccessGuard accessGuard) {
        this.engineService = engineService;
        this.accessGuard = accessGuard;
    }

    @GetMapping
    public BenchmarkDto get(@RequestParam Long branchId, @AuthenticationPrincipal AuthenticatedUser principal) {
        Branch branch = accessGuard.requireBranch(principal, branchId);
        CategoryBenchmark bm = engineService.resolveBenchmark(branch.getOrganization().getCategory());
        return new BenchmarkDto(
                bm.getTargetMarginPercent(),
                bm.getTargetMarginPercent() - 5.0,
                bm.getTargetMarginPercent() + 10.0,
                bm.getLiquidityRatioMin(),
                bm.getLiquidityRatioMax()
        );
    }
}
