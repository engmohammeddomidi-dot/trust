package com.trust.service;

import com.trust.domain.ExpenseCategory;
import com.trust.domain.MonthlyExpense;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * تحويل المصاريف الشهرية إلى مصاريف الفترة التي يقيسها BHI.
 *
 * الدقّة المهمة هنا: المصاريف تتراكم على أيام التقويم، بينما طول فترة BHI هو عدد
 * الأيام المُدخَلة فعليًا. لو أدخل صاحب المحل سبعة أيام من أصل ثلاثين، فمبيعاته
 * المحسوبة تخص تلك السبعة، ويجب أن تخصّها المصاريف أيضًا - وإلا قُورنت مصاريف شهر
 * كامل بمبيعات أسبوع وظهر هامش صافٍ سالب بلا سبب حقيقي.
 *
 * التحقق المرجعي: مبيعات 105,000 ومصاريف 16,800 شهريًا تعطي نسبة 0.16. وبسبعة أيام
 * مُدخَلة: مبيعات 24,500 ومصاريف 3,920 - النسبة تبقى 0.16 تمامًا.
 */
class MonthlyExpenseProrationTest {

    private final MonthlyExpenseService service = new MonthlyExpenseService(null);

    private MonthlyExpense expense(LocalDate month, ExpenseCategory category, double unit, int quantity) {
        MonthlyExpense e = new MonthlyExpense();
        e.setExpenseMonth(month);
        e.setCategory(category);
        e.setUnitAmount(unit);
        e.setQuantity(quantity);
        return e;
    }

    /** جدول المصاريف الحرفي من نموذج مدير المنتج - إجماليه 16,800 */
    private List<MonthlyExpense> referenceSchedule(LocalDate month) {
        return List.of(
                expense(month, ExpenseCategory.MANAGER_SALARY, 5000, 1),
                expense(month, ExpenseCategory.STAFF_SALARY, 2000, 3),
                expense(month, ExpenseCategory.ELECTRICITY, 2500, 1),
                expense(month, ExpenseCategory.RENT, 3000, 1),
                expense(month, ExpenseCategory.TECH_SERVICES, 200, 1),
                expense(month, ExpenseCategory.SUNDRIES, 100, 1));
    }

    @Test
    void monthlyTotal_multipliesUnitAmountByQuantity() {
        LocalDate month = LocalDate.of(2026, 8, 1);

        assertThat(service.monthlyTotal(referenceSchedule(month)))
                .isCloseTo(16_800, within(1e-9)); // 5000 + 3×2000 + 2500 + 3000 + 200 + 100
    }

    @Test
    void aFullMonthOfRecordedDays_costsTheWholeMonthlyTotal() {
        LocalDate month = LocalDate.of(2026, 8, 1);

        assertThat(service.proratedForPeriod(referenceSchedule(month), 30))
                .isCloseTo(16_800, within(1e-9));
    }

    @Test
    void sevenRecordedDays_costSevenThirtiethsSoTheExpenseRatioStaysTrue() {
        LocalDate month = LocalDate.of(2026, 8, 1);

        assertThat(service.proratedForPeriod(referenceSchedule(month), 7))
                .isCloseTo(3_920, within(1e-9)); // 16,800 × 7/30
    }

    @Test
    void periodSpanningTwoMonths_usesTheAverageMonthlyTotal() {
        List<MonthlyExpense> twoMonths = List.of(
                expense(LocalDate.of(2026, 7, 1), ExpenseCategory.RENT, 3000, 1),
                expense(LocalDate.of(2026, 8, 1), ExpenseCategory.RENT, 5000, 1));

        // متوسط الشهرين 4000، ولثلاثين يومًا مُدخَلة = 4000
        assertThat(service.proratedForPeriod(twoMonths, 30)).isCloseTo(4_000, within(1e-9));
    }

    @Test
    void noExpensesRecorded_yieldsNullSoTheIndicatorStaysHonestlyUnavailable() {
        assertThat(service.proratedForPeriod(List.of(), 30)).isNull();
    }

    @Test
    void zeroRecordedDays_yieldsNullRatherThanZeroExpenses() {
        LocalDate month = LocalDate.of(2026, 8, 1);

        assertThat(service.proratedForPeriod(referenceSchedule(month), 0)).isNull();
    }
}
