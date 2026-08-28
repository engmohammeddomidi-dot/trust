package com.trust.service;

import com.trust.domain.BhiDirection;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * اختبار محرك تسجيل مؤشر صحة الأعمال (BHI) مقابل النموذج المرجعي الذي أرسله مدير المنتج
 * (ملف اقتصاديات/BHI). القيم المتوقعة أدناه ليست تقديرية - هي المخرجات الحرفية لورقة
 * "الحساب" في ذلك الملف، لكل مؤشر من المؤشرات الثلاثة عشر. أي انحراف يعني أن تطبيقنا
 * لا يطابق النموذج المعتمد.
 *
 * الصيغة المستخرجة: خطية متعددة القطع بين ثلاث نقاط ارتكاز - ضعيف=40، متوسط=70، ممتاز=100.
 */
class BhiScoringEngineTest {

    private final BhiScoringEngine engine = new BhiScoringEngine();

    private double score(BhiDirection direction, double weak, double medium, double excellent, double value) {
        return engine.normalize(direction, weak, medium, excellent, value);
    }

    // ---------- المؤشرات الثلاثة عشر من ورقة "الحساب" ----------

    @Test
    void netProfitMargin_matchesReferenceModel() {
        assertThat(score(BhiDirection.HIGHER_BETTER, 0.00, 0.02, 0.05, 0.04)).isCloseTo(90.0, within(0.0001));
    }

    @Test
    void grossProfitMargin_matchesReferenceModel() {
        assertThat(score(BhiDirection.HIGHER_BETTER, 0.15, 0.20, 0.28, 0.225)).isCloseTo(79.375, within(0.0001));
    }

    @Test
    void operatingExpenseRatio_matchesReferenceModel() {
        assertThat(score(BhiDirection.LOWER_BETTER, 0.25, 0.18, 0.12, 0.16)).isCloseTo(80.0, within(0.0001));
    }

    @Test
    void currentRatio_matchesReferenceModel() {
        assertThat(score(BhiDirection.HIGHER_BETTER, 0.80, 1.20, 1.80, 1.198347)).isCloseTo(69.876033, within(0.0001));
    }

    @Test
    void cashRatio_matchesReferenceModel() {
        assertThat(score(BhiDirection.HIGHER_BETTER, 0.10, 0.25, 0.50, 0.181818)).isCloseTo(56.363636, within(0.0001));
    }

    @Test
    void cashConversionCycle_matchesReferenceModel() {
        assertThat(score(BhiDirection.LOWER_BETTER, 60, 35, 15, 30)).isCloseTo(77.5, within(0.0001));
    }

    @Test
    void salesGrowth_matchesReferenceModel() {
        assertThat(score(BhiDirection.HIGHER_BETTER, -0.05, 0.03, 0.10, 0.061947)).isCloseTo(83.69153, within(0.0001));
    }

    @Test
    void inventoryTurnover_matchesReferenceModel() {
        assertThat(score(BhiDirection.HIGHER_BETTER, 4, 8, 12, 7.5)).isCloseTo(66.25, within(0.0001));
    }

    @Test
    void wasteRatio_matchesReferenceModel() {
        assertThat(score(BhiDirection.LOWER_BETTER, 0.05, 0.02, 0.005, 0.021)).isCloseTo(69.0, within(0.0001));
    }

    @Test
    void stockAccuracy_matchesReferenceModel() {
        assertThat(score(BhiDirection.HIGHER_BETTER, 0.85, 0.93, 0.98, 0.91)).isCloseTo(62.5, within(0.0001));
    }

    @Test
    void daysSalesOutstanding_matchesReferenceModel() {
        assertThat(score(BhiDirection.LOWER_BETTER, 60, 30, 15, 22)).isCloseTo(86.0, within(0.0001));
    }

    @Test
    void paymentEfficiency_matchesReferenceModel() {
        assertThat(score(BhiDirection.HIGHER_BETTER, 0.60, 0.80, 0.95, 0.82)).isCloseTo(74.0, within(0.0001));
    }

    @Test
    void debtToEquity_matchesReferenceModel() {
        assertThat(score(BhiDirection.LOWER_BETTER, 1.50, 0.70, 0.30, 0.55)).isCloseTo(81.25, within(0.0001));
    }

    // ---------- سلوك الحدود ----------

    @Test
    void valueAtExcellentThreshold_scoresFullMarks() {
        assertThat(score(BhiDirection.HIGHER_BETTER, 4, 8, 12, 12)).isEqualTo(100.0);
    }

    @Test
    void valueBeyondExcellentThreshold_isCappedAtHundred() {
        assertThat(score(BhiDirection.HIGHER_BETTER, 4, 8, 12, 40)).isEqualTo(100.0);
    }

    @Test
    void valueAtMediumThreshold_scoresSeventy() {
        assertThat(score(BhiDirection.HIGHER_BETTER, 4, 8, 12, 8)).isEqualTo(70.0);
    }

    @Test
    void valueAtWeakThreshold_scoresForty() {
        assertThat(score(BhiDirection.HIGHER_BETTER, 4, 8, 12, 4)).isEqualTo(40.0);
    }

    /**
     * النموذج المرجعي لا يحتوي أي مثال تحت الحد الضعيف، فاجتهدنا: استمرار خطي بنفس ميل
     * قطعة (ضعيف→متوسط) نزولًا من 40 نحو الصفر. القيمة 0 عند مسافة قدرها 4/3 من طول
     * تلك القطعة تحت الحد الضعيف.
     */
    @Test
    void valueBelowWeakThreshold_extrapolatesDownFromForty() {
        // القطعة ضعيف→متوسط طولها 4 وحدات (4→8) وتساوي 30 درجة، أي 7.5 درجة لكل وحدة.
        // قيمة 2 تقع وحدتين تحت الحد الضعيف => 40 - (2 × 7.5) = 25
        assertThat(score(BhiDirection.HIGHER_BETTER, 4, 8, 12, 2)).isCloseTo(25.0, within(0.0001));
    }

    @Test
    void valueFarBelowWeakThreshold_isFlooredAtZero() {
        assertThat(score(BhiDirection.HIGHER_BETTER, 4, 8, 12, -100)).isEqualTo(0.0);
    }

    @Test
    void lowerBetterIndicator_belowWeakThresholdMeansWorse() {
        // أقل أفضل: الحد الضعيف 0.05 والمتوسط 0.02 => قيمة 0.08 أسوأ من الضعيف
        assertThat(score(BhiDirection.LOWER_BETTER, 0.05, 0.02, 0.005, 0.08)).isLessThan(40.0);
    }
}
