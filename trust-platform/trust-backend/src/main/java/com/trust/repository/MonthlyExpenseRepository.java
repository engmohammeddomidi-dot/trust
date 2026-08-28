package com.trust.repository;

import com.trust.domain.ExpenseCategory;
import com.trust.domain.MonthlyExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MonthlyExpenseRepository extends JpaRepository<MonthlyExpense, Long> {

    List<MonthlyExpense> findByBranchIdAndExpenseMonth(Long branchId, LocalDate expenseMonth);

    List<MonthlyExpense> findByBranchIdAndExpenseMonthBetween(Long branchId, LocalDate from, LocalDate to);

    Optional<MonthlyExpense> findByBranchIdAndExpenseMonthAndCategory(
            Long branchId, LocalDate expenseMonth, ExpenseCategory category);
}
