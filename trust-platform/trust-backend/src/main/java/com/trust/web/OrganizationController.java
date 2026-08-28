package com.trust.web;

import com.trust.config.AuthenticatedUser;
import com.trust.config.TenantAccessGuard;
import com.trust.domain.Branch;
import com.trust.domain.Organization;
import com.trust.repository.BranchRepository;
import com.trust.repository.OrganizationRepository;
import com.trust.web.dto.BranchDto;
import com.trust.web.dto.BranchUpdateRequest;
import com.trust.web.dto.OrganizationDto;
import com.trust.web.dto.OrganizationUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class OrganizationController {

    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final TenantAccessGuard accessGuard;

    public OrganizationController(OrganizationRepository organizationRepository, BranchRepository branchRepository,
                                   TenantAccessGuard accessGuard) {
        this.organizationRepository = organizationRepository;
        this.branchRepository = branchRepository;
        this.accessGuard = accessGuard;
    }

    @GetMapping("/api/organizations/{id}")
    public OrganizationDto getOrganization(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireOrganization(principal, id);
        return toDto(organizationRepository.findById(id).orElseThrow());
    }

    @PutMapping("/api/organizations/{id}")
    public OrganizationDto updateOrganization(@PathVariable Long id, @Valid @RequestBody OrganizationUpdateRequest request,
                                               @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireOrganization(principal, id);
        Organization org = organizationRepository.findById(id).orElseThrow();
        org.setName(request.name());
        // فارغة تعني "لم تُسجَّل بعد" ويبقى مؤشر نسبة الدين غير متاح - وهذا مقصود
        org.setEquity(request.equity());
        return toDto(organizationRepository.save(org));
    }

    @GetMapping("/api/branches")
    public List<BranchDto> listBranches(@RequestParam Long organizationId, @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireOrganization(principal, organizationId);
        return branchRepository.findByOrganizationId(organizationId).stream().map(OrganizationController::toDto).toList();
    }

    @PutMapping("/api/branches/{id}")
    public BranchDto updateBranch(@PathVariable Long id, @Valid @RequestBody BranchUpdateRequest request,
                                   @AuthenticationPrincipal AuthenticatedUser principal) {
        Branch branch = accessGuard.requireBranch(principal, id);
        branch.setName(request.name());
        branch.setCity(request.city());
        branch.setActive(request.active());
        return toDto(branchRepository.save(branch));
    }

    private static OrganizationDto toDto(Organization org) {
        return new OrganizationDto(org.getId(), org.getName(), org.getCategory().name(), org.getEquity());
    }

    private static BranchDto toDto(Branch branch) {
        return new BranchDto(branch.getId(), branch.getOrganization().getId(), branch.getName(), branch.getCity(), branch.isActive());
    }
}
