package com.trust.service;

import com.trust.domain.Branch;
import com.trust.domain.ExpenseCategory;
import com.trust.domain.MonthlyExpense;
import com.trust.repository.MonthlyExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * المصاريف التشغيلية الشهرية، ومنها المصاريف المنسوبة لفترة قياس BHI.
 */
@Service
public class MonthlyExpenseService {

    /** شهر اسمي من ثلاثين يومًا - يبقي حساب النسبة متسقًا مهما اختلف طول الشهر الفعلي */
    private static final double NOMINAL_MONTH_DAYS = 30.0;

    private final MonthlyExpenseRepository repository;

    public MonthlyExpenseService(MonthlyExpenseRepository repository) {
        this.repository = repository;
    }

    public List<MonthlyExpense> listForMonth(Long branchId, LocalDate month) {
        return repository.findByBranchIdAndExpenseMonth(branchId, firstOfMonth(month));
    }

    /** إدخال أو تحديث بند واحد - صف واحد لكل (فرع، شهر، بند) */
    @Transactional
    public MonthlyExpense upsert(Branch branch, LocalDate month, ExpenseCategory category,
                                 double unitAmount, int quantity, String note) {
        LocalDate normalised = firstOfMonth(month);
        MonthlyExpense expense = repository
                .findByBranchIdAndExpenseMonthAndCategory(branch.getId(), normalised, category)
                .orElseGet(() -> {
                    MonthlyExpense e = new MonthlyExpense();
                    e.setBranch(branch);
                    e.setExpenseMonth(normalised);
                    e.setCategory(category);
                    return e;
                });
        expense.setUnitAmount(unitAmount);
        expense.setQuantity(quantity);
        expense.setNote(note);
        return repository.save(expense);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    public double monthlyTotal(Collection<MonthlyExpense> expenses) {
        return expenses.stream().mapToDouble(MonthlyExpense::getTotal).sum();
    }

    /**
     * المصاريف المنسوبة لفترة قياس طولها recordedDays يومًا مُدخَلة.
     *
     * المصاريف تتراكم على أيام التقويم بينما فترة BHI تُقاس بالأيام المُدخَلة فعليًا،
     * لذا نأخذ متوسط الإجمالي الشهري ونضربه في (الأيام المُدخَلة ÷ 30). بدون ذلك
     * تُقارَن مصاريف شهر كامل بمبيعات أسبوع فيظهر هامش صافٍ سالب بلا سبب حقيقي.
     *
     * تعيد null حين لا توجد مصاريف مسجّلة - المؤشر يبقى "غير متاح" بصدق بدل صفر.
     */
    public Double proratedForPeriod(Collection<MonthlyExpense> expenses, int recordedDays) {
        if (expenses.isEmpty() || recordedDays <= 0) return null;

        Map<LocalDate, Double> totalByMonth = new LinkedHashMap<>();
        for (MonthlyExpense e : expenses) {
            totalByMonth.merge(e.getExpenseMonth(), e.getTotal(), Double::sum);
        }

        double averageMonth = totalByMonth.values().stream()
                .mapToDouble(Double::doubleValue).average().orElse(0);

        return averageMonth * (recordedDays / NOMINAL_MONTH_DAYS);
    }

    /** المصاريف المنسوبة لفترة، مقروءة مباشرةً من قاعدة البيانات */
    public Double proratedForPeriod(Long branchId, LocalDate from, LocalDate to, int recordedDays) {
        List<MonthlyExpense> expenses = repository.findByBranchIdAndExpenseMonthBetween(
                branchId, firstOfMonth(from), firstOfMonth(to));
        return proratedForPeriod(expenses, recordedDays);
    }

    private LocalDate firstOfMonth(LocalDate date) {
        return date.withDayOfMonth(1);
    }
}
