package com.trust.web;

import com.trust.config.AuthenticatedUser;
import com.trust.config.TenantAccessGuard;
import com.trust.domain.Policy;
import com.trust.service.PolicyService;
import com.trust.web.dto.PolicyDto;
import com.trust.web.dto.PolicyUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;
    private final TenantAccessGuard accessGuard;

    public PolicyController(PolicyService policyService, TenantAccessGuard accessGuard) {
        this.policyService = policyService;
        this.accessGuard = accessGuard;
    }

    @GetMapping
    public PolicyDto get(@RequestParam Long organizationId, @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireOrganization(principal, organizationId);
        return toDto(policyService.resolveForOrganization(organizationId));
    }

    @PutMapping
    public PolicyDto update(@RequestParam Long organizationId, @Valid @RequestBody PolicyUpdateRequest request,
                             @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireOrganization(principal, organizationId);
        return toDto(policyService.upsert(organizationId, request.maxPurchaseLiquidityRatio(), request.minSupplierRating()));
    }

    private static PolicyDto toDto(Policy p) {
        return new PolicyDto(p.getMaxPurchaseLiquidityRatio(), p.getMinSupplierRating());
    }
}
