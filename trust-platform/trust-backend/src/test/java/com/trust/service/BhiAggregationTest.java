package com.trust.service;

import com.trust.domain.BhiAxis;
import com.trust.domain.BhiIndicatorCode;
import com.trust.service.BhiScoringEngine.IndicatorInput;
import com.trust.web.dto.BhiResultDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * تجميع درجات المؤشرات إلى محاور ثم إلى المؤشر العام BHI، مقابل النموذج المرجعي.
 *
 * قاعدتان مستخرجتان من ورقة الحساب بالحساب العكسي:
 *  - داخل المحور: وزن متساوٍ (عمود "الوزن داخل المحور" في الملف يحوي الاتجاه لا الوزن).
 *  - بين المحاور: أوزان صريحة 0.30 / 0.20 / 0.20 / 0.15 / 0.15.
 */
class BhiAggregationTest {

    private final BhiScoringEngine engine = new BhiScoringEngine();

    /** الأوزان المرجعية بين المحاور */
    private Map<BhiAxis, Double> referenceWeights() {
        Map<BhiAxis, Double> w = new LinkedHashMap<>();
        w.put(BhiAxis.PROFITABILITY, 0.30);
        w.put(BhiAxis.LIQUIDITY, 0.20);
        w.put(BhiAxis.OPERATIONAL_EFFICIENCY, 0.20);
        w.put(BhiAxis.INVENTORY_MANAGEMENT, 0.15);
        w.put(BhiAxis.RECEIVABLES_DEBT, 0.15);
        return w;
    }

    /** المدخلات الثلاثة عشر الحرفية من ورقتي المدخلات والمعايير */
    private List<IndicatorInput> referenceInputs() {
        List<IndicatorInput> in = new ArrayList<>();
        in.add(new IndicatorInput(BhiIndicatorCode.NET_PROFIT_MARGIN, 0.04, 0.00, 0.02, 0.05));
        in.add(new IndicatorInput(BhiIndicatorCode.GROSS_PROFIT_MARGIN, 0.225, 0.15, 0.20, 0.28));
        in.add(new IndicatorInput(BhiIndicatorCode.OPERATING_EXPENSE_RATIO, 0.16, 0.25, 0.18, 0.12));
        in.add(new IndicatorInput(BhiIndicatorCode.CURRENT_RATIO, 1.198347, 0.80, 1.20, 1.80));
        in.add(new IndicatorInput(BhiIndicatorCode.CASH_RATIO, 0.181818, 0.10, 0.25, 0.50));
        in.add(new IndicatorInput(BhiIndicatorCode.CASH_CONVERSION_CYCLE, 30.0, 60, 35, 15));
        in.add(new IndicatorInput(BhiIndicatorCode.SALES_GROWTH, 0.061947, -0.05, 0.03, 0.10));
        in.add(new IndicatorInput(BhiIndicatorCode.INVENTORY_TURNOVER, 7.5, 4, 8, 12));
        in.add(new IndicatorInput(BhiIndicatorCode.WASTE_RATIO, 0.021, 0.05, 0.02, 0.005));
        in.add(new IndicatorInput(BhiIndicatorCode.STOCK_ACCURACY, 0.91, 0.85, 0.93, 0.98));
        in.add(new IndicatorInput(BhiIndicatorCode.DAYS_SALES_OUTSTANDING, 22.0, 60, 30, 15));
        in.add(new IndicatorInput(BhiIndicatorCode.PAYMENT_EFFICIENCY, 0.82, 0.60, 0.80, 0.95));
        in.add(new IndicatorInput(BhiIndicatorCode.DEBT_TO_EQUITY, 0.55, 1.50, 0.70, 0.30));
        return in;
    }

    private Double axisScore(BhiResultDto result, BhiAxis axis) {
        return result.axes().stream()
                .filter(a -> a.axis() == axis)
                .findFirst().orElseThrow()
                .score();
    }

    @Test
    void overallIndex_matchesReferenceModel() {
        BhiResultDto result = engine.aggregate(referenceInputs(), referenceWeights());
        assertThat(result.totalScore()).isCloseTo(77.208451, within(0.0001));
    }

    @Test
    void eachAxisScore_matchesReferenceModel() {
        BhiResultDto result = engine.aggregate(referenceInputs(), referenceWeights());
        assertThat(axisScore(result, BhiAxis.PROFITABILITY)).isCloseTo(83.125, within(0.0001));
        assertThat(axisScore(result, BhiAxis.LIQUIDITY)).isCloseTo(67.913223, within(0.0001));
        assertThat(axisScore(result, BhiAxis.OPERATIONAL_EFFICIENCY)).isCloseTo(83.69153, within(0.0001));
        assertThat(axisScore(result, BhiAxis.INVENTORY_MANAGEMENT)).isCloseTo(65.916667, within(0.0001));
        assertThat(axisScore(result, BhiAxis.RECEIVABLES_DEBT)).isCloseTo(80.416667, within(0.0001));
    }

    @Test
    void classification_matchesReferenceModel() {
        BhiResultDto result = engine.aggregate(referenceInputs(), referenceWeights());
        assertThat(result.label()).isEqualTo("جيدة");
    }

    // ---------- التعامل مع المؤشرات غير المتاحة (بيانات ناقصة) ----------

    @Test
    void unavailableIndicator_isExcludedAndItsAxisRenormalisesOverTheRest() {
        List<IndicatorInput> inputs = referenceInputs();
        // إسقاط هامش صافي الربح لأنه يتطلب المصاريف التشغيلية غير المتوفرة بعد
        inputs.removeIf(i -> i.code() == BhiIndicatorCode.NET_PROFIT_MARGIN);
        inputs.add(new IndicatorInput(BhiIndicatorCode.NET_PROFIT_MARGIN, null, 0.00, 0.02, 0.05));

        BhiResultDto result = engine.aggregate(inputs, referenceWeights());

        // الربحية تصبح متوسط المؤشرين المتبقيين فقط: (79.375 + 80) / 2
        assertThat(axisScore(result, BhiAxis.PROFITABILITY)).isCloseTo(79.6875, within(0.0001));
    }

    @Test
    void unavailableIndicator_isStillReportedSoTheUserSeesWhatIsMissing() {
        List<IndicatorInput> inputs = referenceInputs();
        inputs.removeIf(i -> i.code() == BhiIndicatorCode.NET_PROFIT_MARGIN);
        inputs.add(new IndicatorInput(BhiIndicatorCode.NET_PROFIT_MARGIN, null, 0.00, 0.02, 0.05));

        BhiResultDto result = engine.aggregate(inputs, referenceWeights());

        var profitability = result.axes().stream()
                .filter(a -> a.axis() == BhiAxis.PROFITABILITY).findFirst().orElseThrow();
        var missing = profitability.indicators().stream()
                .filter(i -> i.code() == BhiIndicatorCode.NET_PROFIT_MARGIN).findFirst().orElseThrow();

        assertThat(missing.available()).isFalse();
        assertThat(missing.score()).isNull();
        assertThat(result.availableIndicatorCount()).isEqualTo(12);
        assertThat(result.totalIndicatorCount()).isEqualTo(13);
    }

    @Test
    void axisWithNoAvailableIndicators_isExcludedFromTheTotalAndWeightsRenormalise() {
        // إبقاء محور الربحية وحده متاحًا يعني أن المؤشر العام يساوي درجته تمامًا
        List<IndicatorInput> inputs = new ArrayList<>();
        inputs.add(new IndicatorInput(BhiIndicatorCode.NET_PROFIT_MARGIN, 0.04, 0.00, 0.02, 0.05));
        inputs.add(new IndicatorInput(BhiIndicatorCode.GROSS_PROFIT_MARGIN, 0.225, 0.15, 0.20, 0.28));
        inputs.add(new IndicatorInput(BhiIndicatorCode.OPERATING_EXPENSE_RATIO, 0.16, 0.25, 0.18, 0.12));
        inputs.add(new IndicatorInput(BhiIndicatorCode.CURRENT_RATIO, null, 0.80, 1.20, 1.80));
        inputs.add(new IndicatorInput(BhiIndicatorCode.SALES_GROWTH, null, -0.05, 0.03, 0.10));

        BhiResultDto result = engine.aggregate(inputs, referenceWeights());

        assertThat(result.totalScore()).isCloseTo(83.125, within(0.0001));
    }

    /**
     * محور بلا بيانات لا يُحذف من المخرجات بصمت - يظهر بدرجة فارغة. اختفاؤه كان يعني
     * أن صاحب المحل لا يعرف أصلًا أن هناك محورًا لم يُقيَّم (حدث فعليًا مع محور الكفاءة
     * التشغيلية في بيانات تجريبية لا تغطي فترة سابقة).
     */
    @Test
    void axisWithNoAvailableIndicators_isStillListedWithAnEmptyScore() {
        List<IndicatorInput> inputs = new ArrayList<>();
        inputs.add(new IndicatorInput(BhiIndicatorCode.GROSS_PROFIT_MARGIN, 0.225, 0.15, 0.20, 0.28));
        inputs.add(new IndicatorInput(BhiIndicatorCode.SALES_GROWTH, null, -0.05, 0.03, 0.10));

        BhiResultDto result = engine.aggregate(inputs, referenceWeights());

        assertThat(axisScore(result, BhiAxis.OPERATIONAL_EFFICIENCY)).isNull();
        assertThat(axisScore(result, BhiAxis.PROFITABILITY)).isNotNull();
    }

    @Test
    void noAvailableIndicatorsAtAll_yieldsUnknownRatherThanAFabricatedScore() {
        List<IndicatorInput> inputs = List.of(
                new IndicatorInput(BhiIndicatorCode.NET_PROFIT_MARGIN, null, 0.00, 0.02, 0.05));

        BhiResultDto result = engine.aggregate(inputs, referenceWeights());

        assertThat(result.totalScore()).isNull();
        assertThat(result.label()).isEqualTo("غير كافٍ");
    }

    // ---------- التصنيف ----------

    @Test
    void classificationBands_useTheSameAnchorsAsTheScoringScale() {
        assertThat(engine.classify(90.0)).isEqualTo("ممتازة");
        assertThat(engine.classify(85.0)).isEqualTo("ممتازة");
        assertThat(engine.classify(70.0)).isEqualTo("جيدة");
        assertThat(engine.classify(55.0)).isEqualTo("مقبولة");
        assertThat(engine.classify(54.9)).isEqualTo("ضعيفة");
    }

    // ---------- الشرح المقروء (Explainable AI) ----------

    @Test
    void eachIndicator_carriesAPlainLanguageExplanationOfItsBand() {
        BhiResultDto result = engine.aggregate(referenceInputs(), referenceWeights());

        var turnover = result.axes().stream()
                .filter(a -> a.axis() == BhiAxis.INVENTORY_MANAGEMENT).findFirst().orElseThrow()
                .indicators().stream()
                .filter(i -> i.code() == BhiIndicatorCode.INVENTORY_TURNOVER).findFirst().orElseThrow();

        assertThat(turnover.explanation())
                .contains("7.5")
                .contains("4")
                .contains("8");
    }
}
