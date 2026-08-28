package com.trust.service;

import com.trust.domain.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * المؤشرات الأربعة التي كانت تنتظر مصادر بيانات: كفاءة السداد، الهدر، دقة الجرد،
 * ونسبة الدين إلى حقوق الملكية.
 */
class BhiRemainingIndicatorsTest {

    private final BhiService service = new BhiService(null, null, null, null, null, null, null,
            new MonthlyExpenseService(null), new BhiMetricsCalculator(), new BhiScoringEngine());

    private Purchase purchase(LocalDate due, LocalDate paid) {
        Purchase p = new Purchase();
        p.setPurchaseDate(LocalDate.of(2026, 8, 1));
        p.setPaymentDueDate(due);
        p.setPaidOnDate(paid);
        return p;
    }

    // ---------- كفاءة السداد ----------

    @Test
    void paymentEfficiency_isTheShareOfInvoicesSettledByTheirDueDate() {
        List<Purchase> purchases = List.of(
                purchase(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 8)),  // مبكر
                purchase(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10)), // في الموعد
                purchase(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 15)), // متأخر
                purchase(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 9)));  // مبكر

        assertThat(service.paymentEfficiency(purchases, LocalDate.of(2026, 8, 20)))
                .isCloseTo(0.75, within(1e-9));
    }

    @Test
    void settlingExactlyOnTheDueDate_countsAsOnTime() {
        List<Purchase> purchases = List.of(
                purchase(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10)));

        assertThat(service.paymentEfficiency(purchases, LocalDate.of(2026, 8, 20)))
                .isCloseTo(1.0, within(1e-9));
    }

    /**
     * فاتورة لم تُسدَّد وتجاوزت استحقاقها هي تأخّر فعلي - لا بيانات ناقصة. تجاهلها
     * كان سيجعل المتعثّر يبدو منضبطًا لمجرد أنه لم يسدّد أصلًا.
     */
    @Test
    void anUnpaidInvoicePastItsDueDate_countsAsLateNotAsMissingData() {
        List<Purchase> purchases = List.of(
                purchase(LocalDate.of(2026, 8, 10), null),
                purchase(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 9)));

        assertThat(service.paymentEfficiency(purchases, LocalDate.of(2026, 8, 20)))
                .isCloseTo(0.5, within(1e-9));
    }

    @Test
    void anUnpaidInvoiceNotYetDue_isExcludedBecauseItsOutcomeIsStillUnknown() {
        List<Purchase> purchases = List.of(
                purchase(LocalDate.of(2026, 8, 25), null),
                purchase(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 9)));

        assertThat(service.paymentEfficiency(purchases, LocalDate.of(2026, 8, 20)))
                .isCloseTo(1.0, within(1e-9));
    }

    @Test
    void purchasesWithoutADueDate_areIgnoredSincePunctualityIsUndefined() {
        List<Purchase> purchases = List.of(
                purchase(null, LocalDate.of(2026, 8, 9)),
                purchase(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 15)));

        assertThat(service.paymentEfficiency(purchases, LocalDate.of(2026, 8, 20)))
                .isCloseTo(0.0, within(1e-9));
    }

    @Test
    void noPurchasesWithDueDates_yieldsNullSoTheIndicatorStaysUnavailable() {
        assertThat(service.paymentEfficiency(List.of(), LocalDate.of(2026, 8, 20))).isNull();
        assertThat(service.paymentEfficiency(List.of(purchase(null, null)), LocalDate.of(2026, 8, 20))).isNull();
    }

    // ---------- نسبة الدين إلى حقوق الملكية ----------

    @Test
    void debtToEquity_dividesLiabilitiesByTheRecordedEquity() {
        assertThat(service.debtToEquity(55_000, 100_000.0)).isCloseTo(0.55, within(1e-9));
    }

    @Test
    void debtToEquity_isUnavailableWhenEquityHasNotBeenRecorded() {
        assertThat(service.debtToEquity(55_000, null)).isNull();
    }

    @Test
    void debtToEquity_isUnavailableWhenEquityIsZeroRatherThanInfinite() {
        assertThat(service.debtToEquity(55_000, 0.0)).isNull();
    }

    // ---------- الهدر ودقة الجرد ----------

    @Test
    void wasteRatio_isWasteValueOverInventoryValue() {
        assertThat(service.wasteRatio(2_100, 100_000)).isCloseTo(0.021, within(1e-9));
    }

    @Test
    void wasteRatio_isUnavailableWithoutInventoryToMeasureAgainst() {
        assertThat(service.wasteRatio(2_100, 0)).isNull();
    }

    /**
     * لا سجل توالف أصلًا يختلف عن سجل قيمته صفر: الأول بيانات ناقصة، والثاني أداء
     * ممتاز يستحق درجة كاملة.
     */
    @Test
    void aStockCountWithNoDiscrepancy_scoresPerfectAccuracyRatherThanReadingAsMissing() {
        StockCount count = new StockCount();
        count.setCountedQuantity(100);
        count.setExpectedQuantity(100);

        assertThat(service.stockAccuracy(List.of(count))).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void stockAccuracy_isOneMinusTheShareOfAbsoluteDiscrepancy() {
        StockCount matching = new StockCount();
        matching.setCountedQuantity(90);
        matching.setExpectedQuantity(100);

        StockCount over = new StockCount();
        over.setCountedQuantity(105);
        over.setExpectedQuantity(100);

        // إجمالي الفارق المطلق 15 على إجمالي متوقَّع 200 => دقة 92.5%
        assertThat(service.stockAccuracy(List.of(matching, over))).isCloseTo(0.925, within(1e-9));
    }

    @Test
    void stockAccuracy_isUnavailableWhenNoCountHasBeenPerformed() {
        assertThat(service.stockAccuracy(List.of())).isNull();
    }
}
