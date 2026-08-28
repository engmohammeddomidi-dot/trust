package com.trust.web;

import com.trust.config.AuthenticatedUser;
import com.trust.config.TenantAccessGuard;
import com.trust.domain.Branch;
import com.trust.domain.Decision;
import com.trust.domain.Supplier;
import com.trust.repository.DecisionRepository;
import com.trust.repository.SupplierRepository;
import com.trust.service.DecisionActionService;
import com.trust.service.DecisionAnalyticsService;
import com.trust.service.PurchaseDecisionEngineService;
import com.trust.web.dto.DecisionDto;
import com.trust.web.dto.DecisionModifyRequest;
import com.trust.web.dto.DecisionQualityScoreDto;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/decisions")
public class DecisionController {

    private final DecisionRepository decisionRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseDecisionEngineService engineService;
    private final DecisionActionService actionService;
    private final DecisionAnalyticsService analyticsService;
    private final TenantAccessGuard accessGuard;

    public DecisionController(DecisionRepository decisionRepository, SupplierRepository supplierRepository,
                               PurchaseDecisionEngineService engineService,
                               DecisionActionService actionService, DecisionAnalyticsService analyticsService,
                               TenantAccessGuard accessGuard) {
        this.decisionRepository = decisionRepository;
        this.supplierRepository = supplierRepository;
        this.engineService = engineService;
        this.actionService = actionService;
        this.analyticsService = analyticsService;
        this.accessGuard = accessGuard;
    }

    @GetMapping
    public List<DecisionDto> list(@RequestParam Long branchId, @RequestParam(required = false) Decision.Status status,
                                   @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireBranch(principal, branchId);
        List<Decision> decisions = status != null
                ? decisionRepository.findByBranchIdAndStatusOrderByFinancialImpactDesc(branchId, status)
                : decisionRepository.findByBranchIdOrderByFinancialImpactDesc(branchId);
        return decisions.stream().map(DecisionController::toDto).toList();
    }

    /** يُعيد توليد قرارات الشراء لفرع معيّن بتشغيل محرك القرار فورًا (بديل مبسّط لجدولة يومية) */
    @PostMapping("/regenerate")
    public List<DecisionDto> regenerate(@RequestParam Long branchId, @AuthenticationPrincipal AuthenticatedUser principal) {
        Branch branch = accessGuard.requireBranch(principal, branchId);
        List<Decision> generated = engineService.generateForBranch(branch);
        return decisionRepository.saveAll(generated).stream().map(DecisionController::toDto).toList();
    }

    @PatchMapping("/{id}/approve")
    public DecisionDto approve(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal) {
        Decision d = requireOwnedDecision(id, principal);
        return toDto(decisionRepository.save(actionService.approve(d)));
    }

    @PatchMapping("/{id}/modify")
    public DecisionDto modify(@PathVariable Long id, @Valid @RequestBody DecisionModifyRequest request,
                               @AuthenticationPrincipal AuthenticatedUser principal) {
        Decision d = requireOwnedDecision(id, principal);
        Supplier supplier = null;
        if (request.supplierId() != null) {
            supplier = supplierRepository.findById(request.supplierId())
                    .orElseThrow(() -> new IllegalArgumentException("المورد غير موجود"));
            if (!supplier.getOrganization().getId().equals(principal.organizationId())) {
                throw new IllegalArgumentException("لا يمكن اختيار مورد من مؤسسة أخرى");
            }
        }
        return toDto(decisionRepository.save(actionService.modify(d, request.quantity(), supplier)));
    }

    @PatchMapping("/{id}/defer")
    public DecisionDto defer(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal) {
        Decision d = requireOwnedDecision(id, principal);
        return toDto(decisionRepository.save(actionService.defer(d)));
    }

    @PatchMapping("/{id}/dismiss")
    public DecisionDto dismiss(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal) {
        Decision d = requireOwnedDecision(id, principal);
        return toDto(decisionRepository.save(actionService.dismiss(d)));
    }

    /** مؤشر جودة القرار (Decision Quality Score) - نسبة الطلبيات المستلمة بدون أي انحراف عن المتوقع */
    @GetMapping("/quality-score")
    public DecisionQualityScoreDto qualityScore(@RequestParam Long branchId, @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireBranch(principal, branchId);
        return analyticsService.qualityScore(List.of(branchId));
    }

    private Decision requireOwnedDecision(Long id, AuthenticatedUser principal) {
        Decision d = decisionRepository.findById(id).orElseThrow(() -> new NoSuchElementException("القرار غير موجود"));
        accessGuard.requireBranchOwnership(principal, d.getBranch());
        return d;
    }

    private static DecisionDto toDto(Decision d) {
        Supplier supplier = d.getSupplier();
        return new DecisionDto(d.getId(), d.getItem().getId(), d.getItem().getName(),
                supplier != null ? supplier.getId() : null, supplier != null ? supplier.getName() : null,
                d.getType().name(), d.getCategory().name(), d.getStatus().name(), d.getSuggestedQuantity(), d.getApprovedQuantity(),
                d.getReasonSummary(), d.getConfidenceScore(), d.getFinancialImpact(), d.getCreatedAt(), d.getResolvedAt(),
                d.getActualOutcome(),
                d.getIfIgnoredSummary(), d.getConstraintsSummary(), d.getConfidenceReasons(),
                parseAlternatives(d.getAlternativesJson()));
    }

    /** البدائل مخزَّنة JSON؛ تلفها لا يجب أن يُسقط عرض القرار كله */
    private static List<DecisionDto.Alternative> parseAlternatives(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return new ObjectMapper().readValue(json, new TypeReference<List<DecisionDto.Alternative>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
