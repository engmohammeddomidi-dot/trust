package com.trust.web;

import com.trust.config.AuthenticatedUser;
import com.trust.config.TenantAccessGuard;
import com.trust.domain.Branch;
import com.trust.domain.ExpenseCategory;
import com.trust.domain.MonthlyExpense;
import com.trust.service.MonthlyExpenseService;
import com.trust.web.dto.ExpenseCategoryDto;
import com.trust.web.dto.MonthlyExpenseDto;
import com.trust.web.dto.MonthlyExpenseRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * المصاريف التشغيلية الشهرية - مدخلها الوحيد في المنصة. هي ما يفتح مؤشرَي هامش صافي
 * الربح ونسبة المصاريف في محور الربحية، أثقل محاور BHI وزنًا (0.30).
 */
@RestController
@RequestMapping("/api/expenses")
public class MonthlyExpenseController {

    private final MonthlyExpenseService expenseService;
    private final TenantAccessGuard accessGuard;

    public MonthlyExpenseController(MonthlyExpenseService expenseService, TenantAccessGuard accessGuard) {
        this.expenseService = expenseService;
        this.accessGuard = accessGuard;
    }

    @GetMapping
    public List<MonthlyExpenseDto> list(@RequestParam Long branchId,
                                        @RequestParam(required = false) String month,
                                        @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireBranch(principal, branchId);
        LocalDate target = month != null ? LocalDate.parse(month) : LocalDate.now();
        return expenseService.listForMonth(branchId, target).stream()
                .map(MonthlyExpenseController::toDto)
                .toList();
    }

    @PutMapping
    public MonthlyExpenseDto upsert(@Valid @RequestBody MonthlyExpenseRequest request,
                                    @AuthenticationPrincipal AuthenticatedUser principal) {
        Branch branch = accessGuard.requireBranch(principal, request.branchId());
        MonthlyExpense saved = expenseService.upsert(branch, LocalDate.parse(request.month()),
                ExpenseCategory.valueOf(request.category()), request.unitAmount(),
                request.quantity(), request.note());
        return toDto(saved);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @RequestParam Long branchId,
                       @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireBranch(principal, branchId);
        expenseService.delete(id);
    }

    /** بنود المصاريف المتاحة للاختيار في الواجهة */
    @GetMapping("/categories")
    public List<ExpenseCategoryDto> categories() {
        return Arrays.stream(ExpenseCategory.values())
                .map(c -> new ExpenseCategoryDto(c.name(), c.getLabelAr()))
                .toList();
    }

    private static MonthlyExpenseDto toDto(MonthlyExpense e) {
        return new MonthlyExpenseDto(e.getId(), e.getExpenseMonth().toString(), e.getCategory().name(),
                e.getCategory().getLabelAr(), e.getUnitAmount(), e.getQuantity(), e.getTotal(), e.getNote());
    }
}
