package com.trust.config;

import com.trust.domain.Branch;
import com.trust.domain.Organization;
import com.trust.repository.BranchRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;

/**
 * يمنع الوصول العابر بين المؤسسات (IDOR): كل نقطة API تستقبل branchId/organizationId
 * من العميل يجب أن تتحقق أن القيمة فعلاً تخص مؤسسة المستخدم المصادَق عليه، وإلا HTTP 403.
 */
@Component
public class TenantAccessGuard {

    private final BranchRepository branchRepository;

    public TenantAccessGuard(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    /** يتحقق من الفرع المطلوب ويعيده إن كان يتبع لمؤسسة المستخدم */
    public Branch requireBranch(AuthenticatedUser principal, Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new NoSuchElementException("الفرع غير موجود"));
        requireBranchOwnership(principal, branch);
        return branch;
    }

    /** يتحقق أن فرعًا مُحمَّلًا مسبقًا (مثلاً عبر Recommendation.getBranch()) يخص المستخدم */
    public void requireBranchOwnership(AuthenticatedUser principal, Branch branch) {
        if (principal.organizationId() == null || !branch.getOrganization().getId().equals(principal.organizationId())) {
            throw new AccessDeniedException("لا تملك صلاحية الوصول لهذا الفرع");
        }
    }

    /** يتحقق أن المؤسسة المطلوبة هي نفس مؤسسة المستخدم */
    public void requireOrganization(AuthenticatedUser principal, Long organizationId) {
        if (principal.organizationId() == null || !organizationId.equals(principal.organizationId())) {
            throw new AccessDeniedException("لا تملك صلاحية الوصول لهذه المؤسسة");
        }
    }

    public void requireOrganizationOwnership(AuthenticatedUser principal, Organization organization) {
        requireOrganization(principal, organization.getId());
    }
}
