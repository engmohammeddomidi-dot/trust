package com.trust.config;

import com.trust.domain.Branch;
import com.trust.domain.Organization;
import com.trust.repository.BranchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * اختبار وحدة لحارس منع الوصول العابر بين المؤسسات (IDOR) - أهم آلية أمان في نظام
 * متعدد المستأجرين. أي انحراف هنا يعني تسرّب بيانات مؤسسة لأخرى.
 */
class TenantAccessGuardTest {

    private final BranchRepository branchRepository = mock(BranchRepository.class);
    private final TenantAccessGuard guard = new TenantAccessGuard(branchRepository);

    private AuthenticatedUser userOf(long organizationId) {
        return new AuthenticatedUser(1L, "user@test.com", "OWNER", organizationId, 10L);
    }

    private Branch branchOf(long organizationId) {
        Organization org = new Organization();
        org.setId(organizationId);
        Branch branch = new Branch();
        branch.setId(99L);
        branch.setOrganization(org);
        return branch;
    }

    @Test
    void requireBranch_returnsBranch_whenItBelongsToUsersOrganization() {
        Branch branch = branchOf(5L);
        when(branchRepository.findById(99L)).thenReturn(Optional.of(branch));

        Branch result = guard.requireBranch(userOf(5L), 99L);

        assertThat(result).isSameAs(branch);
    }

    @Test
    void requireBranch_throwsAccessDenied_whenBranchBelongsToAnotherOrganization() {
        Branch branch = branchOf(5L);
        when(branchRepository.findById(99L)).thenReturn(Optional.of(branch));

        assertThatThrownBy(() -> guard.requireBranch(userOf(7L), 99L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireBranch_throwsNotFound_whenBranchDoesNotExist() {
        when(branchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.requireBranch(userOf(5L), 99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void requireOrganization_throwsAccessDenied_whenPrincipalHasNoOrganization() {
        AuthenticatedUser platformAdmin = new AuthenticatedUser(1L, "admin@trust.demo", "PLATFORM_ADMIN", null, null);

        assertThatThrownBy(() -> guard.requireOrganization(platformAdmin, 5L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireOrganization_passes_whenIdsMatch() {
        guard.requireOrganization(userOf(5L), 5L);
    }
}
