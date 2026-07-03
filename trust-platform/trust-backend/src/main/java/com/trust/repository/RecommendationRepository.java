package com.trust.repository;

import com.trust.domain.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByBranchIdInAndStatusOrderByExpectedValueDesc(List<Long> branchIds, Recommendation.Status status);
    List<Recommendation> findByBranchIdAndStatusOrderByExpectedValueDesc(Long branchId, Recommendation.Status status);
    List<Recommendation> findByBranchIdOrderByExpectedValueDesc(Long branchId);
    List<Recommendation> findByBranchIdInOrderByExpectedValueDesc(List<Long> branchIds);
}
