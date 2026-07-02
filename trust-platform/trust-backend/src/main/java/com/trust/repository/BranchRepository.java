package com.trust.repository;

import com.trust.domain.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByOrganizationId(Long organizationId);
    List<Branch> findByOrganizationIdIn(List<Long> organizationIds);
}
