package com.trust.repository;

import com.trust.domain.DailyEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyEntryRepository extends JpaRepository<DailyEntry, Long> {
    List<DailyEntry> findByBranchIdAndEntryDateBetweenOrderByEntryDateAsc(Long branchId, LocalDate from, LocalDate to);
    List<DailyEntry> findByBranchIdInAndEntryDateBetweenOrderByEntryDateAsc(List<Long> branchIds, LocalDate from, LocalDate to);
    Optional<DailyEntry> findByBranchIdAndEntryDate(Long branchId, LocalDate date);
}
