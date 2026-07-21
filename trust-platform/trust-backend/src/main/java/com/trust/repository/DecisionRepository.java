package com.trust.repository;

import com.trust.domain.Decision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DecisionRepository extends JpaRepository<Decision, Long> {
    List<Decision> findByBranchIdAndStatusOrderByFinancialImpactDesc(Long branchId, Decision.Status status);
    List<Decision> findByBranchIdOrderByFinancialImpactDesc(Long branchId);
    List<Decision> findByBranchIdInAndStatusOrderByFinancialImpactDesc(List<Long> branchIds, Decision.Status status);
    List<Decision> findByBranchIdInOrderByFinancialImpactDesc(List<Long> branchIds);
    List<Decision> findByBranchIdIn(List<Long> branchIds);
}
