package com.trust.repository;

import com.trust.domain.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    List<Purchase> findByBranchIdOrderByPurchaseDateDesc(Long branchId);
    List<Purchase> findByBranchIdInOrderByPurchaseDateDesc(List<Long> branchIds);
    List<Purchase> findByBranchIdAndStatusOrderByPurchaseDateDesc(Long branchId, Purchase.Status status);
    Optional<Purchase> findByDecisionId(Long decisionId);
    List<Purchase> findByBranchIdAndDecisionIsNotNull(Long branchId);
    List<Purchase> findByBranchIdInAndDecisionIsNotNull(List<Long> branchIds);
    List<Purchase> findBySupplierIdInOrderByPurchaseDateDesc(List<Long> supplierIds);

    List<Purchase> findByBranchIdAndPurchaseDateBetween(Long branchId, LocalDate from, LocalDate to);
}
