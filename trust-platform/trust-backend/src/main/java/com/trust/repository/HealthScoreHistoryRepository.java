package com.trust.repository;

import com.trust.domain.HealthScoreHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HealthScoreHistoryRepository extends JpaRepository<HealthScoreHistory, Long> {
    Optional<HealthScoreHistory> findTopByBranchIdOrderByScoreDateDesc(Long branchId);
    Optional<HealthScoreHistory> findByBranchIdAndScoreDate(Long branchId, LocalDate date);
    List<HealthScoreHistory> findByBranchIdInAndScoreDateBetweenOrderByScoreDateAsc(List<Long> branchIds, LocalDate from, LocalDate to);
}
