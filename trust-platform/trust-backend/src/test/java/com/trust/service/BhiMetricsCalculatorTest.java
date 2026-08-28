package com.trust.service;

import com.trust.domain.BhiIndicatorCode;
import com.trust.service.BhiMetricsCalculator.RawInputs;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * اشتقاق القيم الخام للمؤشرات من بيانات المنصة الفعلية (الإدخالات اليومية والأصناف).
 *
 * المثال المستخدم مطابق لأرقام محل واحد في نموذج مدير المنتج: مبيعات شهرية 105,000
 * ومشتريات 84,000 ومصاريف تشغيلية 16,800 - وهي تعطي هامش صافي ربح 4% ونسبة مصاريف 16%،
 * أي نفس قيمتَي ورقة المدخلات في ملف BHI. تطابق المصدرين مقصود وهو تحقق متقاطع مفيد.
 */
class BhiMetricsCalculatorTest {

    private final BhiMetricsCalculator calculator = new BhiMetricsCalculator();

    private RawInputs.Builder baseline() {
        return RawInputs.builder()
                .periodDays(30)
                .currentPeriodSales(105_000)
                .previousPeriodSales(100_000)
                .costOfGoodsSold(84_000)
                .inventoryValue(140_000)
                .availableLiquidity(22_000)
                .receivables(25_000)
                .payables(100_000);
    }

    private Double valueOf(RawInputs inputs, BhiIndicatorCode code) {
        Map<BhiIndicatorCode, Double> values = calculator.compute(inputs);
        return values.get(code);
    }

    // ---------- المؤشرات السبعة القابلة للحساب اليوم ----------

    @Test
    void grossProfitMargin_isProfitOverSales() {
        assertThat(valueOf(baseline().build(), BhiIndicatorCode.GROSS_PROFIT_MARGIN))
                .isCloseTo(0.20, within(1e-9));
    }

    @Test
    void currentRatio_countsLiquidityInventoryAndReceivablesAgainstPayables() {
        // (22,000 + 140,000 + 25,000) / 100,000
        assertThat(valueOf(baseline().build(), BhiIndicatorCode.CURRENT_RATIO))
                .isCloseTo(1.87, within(1e-9));
    }

    @Test
    void cashRatio_countsOnlyCashAgainstPayables() {
        assertThat(valueOf(baseline().build(), BhiIndicatorCode.CASH_RATIO))
                .isCloseTo(0.22, within(1e-9));
    }

    @Test
    void inventoryTurnover_annualisesCostOfGoodsSoldOverInventoryValue() {
        // (84,000 × 365/30) / 140,000
        assertThat(valueOf(baseline().build(), BhiIndicatorCode.INVENTORY_TURNOVER))
                .isCloseTo(7.3, within(1e-9));
    }

    @Test
    void daysSalesOutstanding_isReceivablesOverDailySales() {
        // 25,000 / (105,000 / 30)
        assertThat(valueOf(baseline().build(), BhiIndicatorCode.DAYS_SALES_OUTSTANDING))
                .isCloseTo(7.142857, within(1e-6));
    }

    /**
     * دورة التحويل النقدي = أيام المخزون + أيام التحصيل - أيام السداد.
     * أيام المخزون تُشتق من معدل الدوران نفسه (365 ÷ الدوران) وليست مدخلًا مستقلًا -
     * وهذا يصحّح تناقضًا في النموذج المرجعي، حيث أُدخلت 38 يومًا مع دوران 7.5 (أي 48.7 يومًا).
     */
    @Test
    void cashConversionCycle_derivesInventoryDaysFromTurnoverSoTheTwoNeverContradict() {
        // 365/7.3 = 50 يوم مخزون، + 7.142857 تحصيل، - 35.714286 سداد
        assertThat(valueOf(baseline().build(), BhiIndicatorCode.CASH_CONVERSION_CYCLE))
                .isCloseTo(21.428571, within(1e-6));
    }

    @Test
    void salesGrowth_comparesCurrentPeriodToPrevious() {
        assertThat(valueOf(baseline().build(), BhiIndicatorCode.SALES_GROWTH))
                .isCloseTo(0.05, within(1e-9));
    }

    /**
     * حين تختلف عدد أيام الفترتين، المقارنة تكون بين معدّلَي المبيعات اليومية لا بين
     * المجموعين - وإلا بدت فترة أقصر انكماشًا في المبيعات وهي ليست كذلك.
     */
    @Test
    void salesGrowth_comparesDailyRatesWhenTheTwoPeriodsHaveDifferentLengths() {
        // الحالية: 105,000 على 30 يومًا = 3,500/يوم. السابقة: 35,000 على 10 أيام = 3,500/يوم.
        RawInputs inputs = baseline()
                .periodDays(30)
                .previousPeriodSales(35_000)
                .previousPeriodDays(10)
                .build();

        assertThat(valueOf(inputs, BhiIndicatorCode.SALES_GROWTH)).isCloseTo(0.0, within(1e-9));
    }

    // ---------- المؤشرات التي تنتظر مصادر بيانات لم تُبنَ بعد ----------

    @Test
    void netProfitMargin_isUnavailableWithoutOperatingExpenses() {
        assertThat(valueOf(baseline().build(), BhiIndicatorCode.NET_PROFIT_MARGIN)).isNull();
    }

    @Test
    void operatingExpenseRatio_isUnavailableWithoutOperatingExpenses() {
        assertThat(valueOf(baseline().build(), BhiIndicatorCode.OPERATING_EXPENSE_RATIO)).isNull();
    }

    @Test
    void netProfitMargin_isComputedOnceOperatingExpensesAreKnown() {
        RawInputs inputs = baseline().operatingExpenses(16_800.0).build();
        // (105,000 - 84,000 - 16,800) / 105,000 = 4%  - نفس قيمة ورقة المدخلات المرجعية
        assertThat(valueOf(inputs, BhiIndicatorCode.NET_PROFIT_MARGIN))
                .isCloseTo(0.04, within(1e-9));
    }

    @Test
    void operatingExpenseRatio_isComputedOnceOperatingExpensesAreKnown() {
        RawInputs inputs = baseline().operatingExpenses(16_800.0).build();
        assertThat(valueOf(inputs, BhiIndicatorCode.OPERATING_EXPENSE_RATIO))
                .isCloseTo(0.16, within(1e-9));
    }

    @Test
    void wasteStockAccuracyPaymentEfficiencyAndDebtToEquity_areUnavailableInPhaseOne() {
        RawInputs inputs = baseline().build();
        assertThat(valueOf(inputs, BhiIndicatorCode.WASTE_RATIO)).isNull();
        assertThat(valueOf(inputs, BhiIndicatorCode.STOCK_ACCURACY)).isNull();
        assertThat(valueOf(inputs, BhiIndicatorCode.PAYMENT_EFFICIENCY)).isNull();
        assertThat(valueOf(inputs, BhiIndicatorCode.DEBT_TO_EQUITY)).isNull();
    }

    @Test
    void everyIndicatorCode_hasAnEntrySoNothingIsSilentlyOmitted() {
        Map<BhiIndicatorCode, Double> values = calculator.compute(baseline().build());
        assertThat(values).containsOnlyKeys(BhiIndicatorCode.values());
    }

    // ---------- القسمة على صفر تعني "غير متاح"، لا لانهاية ----------

    @Test
    void zeroPayables_makesLiquidityRatiosUnavailableRatherThanInfinite() {
        RawInputs inputs = baseline().payables(0).build();
        assertThat(valueOf(inputs, BhiIndicatorCode.CURRENT_RATIO)).isNull();
        assertThat(valueOf(inputs, BhiIndicatorCode.CASH_RATIO)).isNull();
    }

    /**
     * انهيار المبيعات إلى صفر هو أعلى إشارة يُفترض أن يلتقطها محور الكفاءة التشغيلية -
     * وكان يُعرض "غير متاح" كأن البيانات ناقصة. يجب أن يُقرأ -100%.
     */
    @Test
    void collapseToZeroSales_readsAsMinusOneHundredPercentNotAsMissingData() {
        RawInputs inputs = baseline().currentPeriodSales(0).build();
        assertThat(valueOf(inputs, BhiIndicatorCode.SALES_GROWTH)).isCloseTo(-1.0, within(1e-9));
    }

    @Test
    void zeroPreviousSales_makesGrowthUnavailableRatherThanInfinite() {
        RawInputs inputs = baseline().previousPeriodSales(0).build();
        assertThat(valueOf(inputs, BhiIndicatorCode.SALES_GROWTH)).isNull();
    }

    @Test
    void zeroInventory_makesTurnoverAndCashCycleUnavailable() {
        RawInputs inputs = baseline().inventoryValue(0).build();
        assertThat(valueOf(inputs, BhiIndicatorCode.INVENTORY_TURNOVER)).isNull();
        assertThat(valueOf(inputs, BhiIndicatorCode.CASH_CONVERSION_CYCLE)).isNull();
    }

    @Test
    void zeroSales_makesMarginAndCollectionDaysUnavailable() {
        RawInputs inputs = baseline().currentPeriodSales(0).build();
        assertThat(valueOf(inputs, BhiIndicatorCode.GROSS_PROFIT_MARGIN)).isNull();
        assertThat(valueOf(inputs, BhiIndicatorCode.DAYS_SALES_OUTSTANDING)).isNull();
    }
}
