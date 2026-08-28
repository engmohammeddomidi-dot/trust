package com.trust.repository;

import com.trust.domain.StockCount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StockCountRepository extends JpaRepository<StockCount, Long> {
    List<StockCount> findByBranchIdAndCountDateBetween(Long branchId, LocalDate from, LocalDate to);
}
