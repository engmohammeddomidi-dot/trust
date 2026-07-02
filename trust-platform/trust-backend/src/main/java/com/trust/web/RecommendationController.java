package com.trust.web;

import com.trust.config.AuthenticatedUser;
import com.trust.config.TenantAccessGuard;
import com.trust.domain.Branch;
import com.trust.domain.Recommendation;
import com.trust.repository.RecommendationRepository;
import com.trust.service.RecommendationEngineService;
import com.trust.web.dto.RecommendationDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationRepository recommendationRepository;
    private final RecommendationEngineService engineService;
    private final TenantAccessGuard accessGuard;

    public RecommendationController(RecommendationRepository recommendationRepository,
                                     RecommendationEngineService engineService,
                                     TenantAccessGuard accessGuard) {
        this.recommendationRepository = recommendationRepository;
        this.engineService = engineService;
        this.accessGuard = accessGuard;
    }

    @GetMapping
    public List<RecommendationDto> list(@RequestParam Long branchId, @RequestParam(required = false) Recommendation.Status status,
                                         @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireBranch(principal, branchId);
        List<Recommendation> recs = status != null
                ? recommendationRepository.findByBranchIdAndStatusOrderByExpectedValueDesc(branchId, status)
                : recommendationRepository.findByBranchIdOrderByExpectedValueDesc(branchId);
        return recs.stream().map(RecommendationController::toDto).toList();
    }

    /** يُعيد توليد التوصيات لفرع معيّن بتشغيل محرك القواعد فورًا (بديل مبسّط لجدولة يومية) */
    @PostMapping("/regenerate")
    public List<RecommendationDto> regenerate(@RequestParam Long branchId, @AuthenticationPrincipal AuthenticatedUser principal) {
        Branch branch = accessGuard.requireBranch(principal, branchId);
        List<Recommendation> generated = engineService.generateForBranch(branch);
        return recommendationRepository.saveAll(generated).stream().map(RecommendationController::toDto).toList();
    }

    @PatchMapping("/{id}/apply")
    public RecommendationDto apply(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal) {
        Recommendation r = recommendationRepository.findById(id).orElseThrow();
        accessGuard.requireBranchOwnership(principal, r.getBranch());
        r.setStatus(Recommendation.Status.APPLIED);
        r.setResolvedAt(java.time.LocalDateTime.now());
        return toDto(recommendationRepository.save(r));
    }

    @PatchMapping("/{id}/dismiss")
    public RecommendationDto dismiss(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal) {
        Recommendation r = recommendationRepository.findById(id).orElseThrow();
        accessGuard.requireBranchOwnership(principal, r.getBranch());
        r.setStatus(Recommendation.Status.DISMISSED);
        r.setResolvedAt(java.time.LocalDateTime.now());
        return toDto(recommendationRepository.save(r));
    }

    private static RecommendationDto toDto(Recommendation r) {
        return new RecommendationDto(r.getId(), r.getType().name(), r.getPriority().name(),
                r.getTitle(), r.getExpectedValue(), r.getStatus().name());
    }
}
