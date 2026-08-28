package com.trust.repository;

import com.trust.domain.WasteRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface WasteRecordRepository extends JpaRepository<WasteRecord, Long> {
    List<WasteRecord> findByBranchIdAndWasteDateBetween(Long branchId, LocalDate from, LocalDate to);
}
