package com.trust.service;

import com.trust.domain.DailyEntry;
import com.trust.domain.Item;
import com.trust.service.BhiMetricsCalculator.RawInputs;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * تجميع مدخلات BHI من الإدخالات اليومية والأصناف.
 *
 * التمييز الجوهري هنا: المبيعات وتكلفة البضاعة تدفقات تُجمَع على طول الفترة، بينما
 * السيولة والذمم المدينة والدائنة أرصدة لحظية تُؤخذ من آخر إدخال فقط. جمع الأرصدة
 * على ثلاثين يومًا يعطي رقمًا أكبر ثلاثين ضعفًا يبدو معقولًا ولا ينبّه أحدًا - ولهذا
 * هذا الاختبار موجود.
 */
class BhiInputAssemblyTest {

    private final BhiService service = new BhiService(null, null, null, null, null, null, null,
            new MonthlyExpenseService(null), new BhiMetricsCalculator(), new BhiScoringEngine());

    private DailyEntry entry(LocalDate date, double sales, double cogs,
                             double liquidity, double receivables, double payables) {
        DailyEntry e = new DailyEntry();
        e.setEntryDate(date);
        e.setTotalSales(sales);
        e.setTotalCogs(cogs);
        e.setTotalProfit(sales - cogs);
        e.setAvailableLiquidity(liquidity);
        e.setReceivables(receivables);
        e.setPayables(payables);
        return e;
    }

    private Item item(double costPrice, double quantity) {
        Item i = new Item();
        i.setCostPrice(costPrice);
        i.setQuantity(quantity);
        return i;
    }

    private List<DailyEntry> currentWindow() {
        return List.of(
                entry(LocalDate.of(2026, 8, 1), 1000, 800, 5000, 2000, 9000),
                entry(LocalDate.of(2026, 8, 2), 1500, 1200, 6000, 2500, 9500),
                // آخر إدخال - أرصدته هي التي يجب أن تُستخدم
                entry(LocalDate.of(2026, 8, 3), 2000, 1600, 7000, 3000, 10000));
    }

    private List<DailyEntry> previousWindow() {
        return List.of(
                entry(LocalDate.of(2026, 7, 1), 900, 700, 4000, 1000, 8000),
                entry(LocalDate.of(2026, 7, 2), 1100, 900, 4500, 1200, 8200));
    }

    @Test
    void salesAndCostOfGoodsSold_areFlowsSummedAcrossTheWholeWindow() {
        RawInputs in = service.assembleInputs(currentWindow(), previousWindow(), List.of());

        assertThat(in.currentPeriodSales()).isCloseTo(4500, within(1e-9)); // 1000+1500+2000
        assertThat(in.costOfGoodsSold()).isCloseTo(3600, within(1e-9));    // 800+1200+1600
    }

    @Test
    void liquidityIsAStockTakenFromTheLatestEntryNotSummed() {
        RawInputs in = service.assembleInputs(currentWindow(), previousWindow(), List.of());

        assertThat(in.availableLiquidity()).isCloseTo(7000, within(1e-9)); // لا 18,000
    }

    @Test
    void receivablesAndPayables_areStocksTakenFromTheLatestEntryNotSummed() {
        RawInputs in = service.assembleInputs(currentWindow(), previousWindow(), List.of());

        assertThat(in.receivables()).isCloseTo(3000, within(1e-9)); // لا 7,500
        assertThat(in.payables()).isCloseTo(10000, within(1e-9));   // لا 28,500
    }

    @Test
    void latestEntryIsChosenByDateNotByListPosition() {
        // قائمة غير مرتبة عمدًا - الأحدث (8/3) في المنتصف
        List<DailyEntry> unordered = List.of(
                entry(LocalDate.of(2026, 8, 1), 1000, 800, 5000, 2000, 9000),
                entry(LocalDate.of(2026, 8, 3), 2000, 1600, 7000, 3000, 10000),
                entry(LocalDate.of(2026, 8, 2), 1500, 1200, 6000, 2500, 9500));

        RawInputs in = service.assembleInputs(unordered, previousWindow(), List.of());

        assertThat(in.availableLiquidity()).isCloseTo(7000, within(1e-9));
    }

    @Test
    void previousPeriodSales_comeFromTheSeparatePriorWindow() {
        RawInputs in = service.assembleInputs(currentWindow(), previousWindow(), List.of());

        assertThat(in.previousPeriodSales()).isCloseTo(2000, within(1e-9)); // 900+1100
    }

    @Test
    void inventoryValue_isSummedAtCostAcrossItems() {
        List<Item> items = List.of(item(10, 100), item(5, 40));

        RawInputs in = service.assembleInputs(currentWindow(), previousWindow(), items);

        assertThat(in.inventoryValue()).isCloseTo(1200, within(1e-9)); // 1000 + 200
    }

    @Test
    void emptyCurrentWindow_yieldsZeroedStocksRatherThanAnException() {
        RawInputs in = service.assembleInputs(List.of(), previousWindow(), List.of());

        assertThat(in.currentPeriodSales()).isZero();
        assertThat(in.availableLiquidity()).isZero();
        assertThat(in.payables()).isZero();
    }

    @Test
    void operatingExpenses_areNotYetAvailableFromAnySource() {
        RawInputs in = service.assembleInputs(currentWindow(), previousWindow(), List.of());

        assertThat(in.operatingExpenses()).isNull();
    }

    /**
     * طول الفترة يُؤخذ من عدد الأيام المُدخَلة فعليًا لا من طول التقويم. إدخال سبعة
     * أيام ضمن نافذة ثلاثين يومًا لا يعني أن ثلاثة وعشرين يومًا كانت بلا مبيعات - وهو
     * ما كان يخفض معدل المبيعات اليومي أربعة أضعاف ويضخّم أيام التحصيل والسداد.
     * نفس العُرف المتبع أصلًا في HealthScoreService.calculatePurchasesScore.
     */
    @Test
    void periodLength_comesFromTheNumberOfRecordedDaysNotTheCalendarSpan() {
        RawInputs in = service.assembleInputs(currentWindow(), previousWindow(), List.of());

        assertThat(in.periodDays()).isEqualTo(3); // ثلاثة إدخالات، لا ثلاثون يومًا
    }

    @Test
    void previousPeriodLength_alsoComesFromItsOwnRecordedDayCount() {
        RawInputs in = service.assembleInputs(currentWindow(), previousWindow(), List.of());

        assertThat(in.previousPeriodDays()).isEqualTo(2);
    }

    @Test
    void emptyWindow_fallsBackToAtLeastOneDaySoNothingDividesByZero() {
        RawInputs in = service.assembleInputs(List.of(), List.of(), List.of());

        assertThat(in.periodDays()).isEqualTo(1);
        assertThat(in.previousPeriodDays()).isEqualTo(1);
    }
}
