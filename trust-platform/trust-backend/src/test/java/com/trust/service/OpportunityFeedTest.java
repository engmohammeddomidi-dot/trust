package com.trust.service;

import com.trust.service.OpportunityFeedService.Signal;
import com.trust.service.OpportunityFeedService.SignalKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * طابور الفرص والمخاطر الموحَّد.
 *
 * اليوم كل إشارة تعيش في شاشتها: قرارات الشراء في صفحة، الأصناف الراكدة في أخرى،
 * قرب انتهاء الصلاحية في ثالثة. رؤية المنتج تريد صفًا واحدًا مرتَّبًا بالأثر المتوقَّع
 * يجيب سؤال "ماذا أفعل اليوم؟" - وهذه الخدمة ترتيب فوق إشارات موجودة، لا محرك جديد.
 */
class OpportunityFeedTest {

    private final OpportunityFeedService service = new OpportunityFeedService();

    private Signal signal(String title, SignalKind kind, double impact, double urgency) {
        return new Signal(kind, title, "", impact, urgency, null, null);
    }

    @Test
    void signalsAreRankedByImpactWeightedByUrgency() {
        List<Signal> ranked = service.rank(List.of(
                signal("متوسط الأثر وعاجل", SignalKind.RISK, 1_000, 1.0),
                signal("عالي الأثر وغير عاجل", SignalKind.OPPORTUNITY, 5_000, 0.1),
                signal("عالي الأثر وعاجل", SignalKind.RISK, 5_000, 1.0)), 5);

        assertThat(ranked).extracting(Signal::title)
                .containsExactly("عالي الأثر وعاجل", "متوسط الأثر وعاجل", "عالي الأثر وغير عاجل");
    }

    /**
     * سقف صارم على الطابور: رؤية المنتج تنصّ على ألا تتجاوز الشاشة الرئيسية خمسة
     * عناصر - "خمسون تنبيهًا تعني أن المستخدم يغلق التطبيق".
     */
    @Test
    void theFeedIsCappedSoTheHomeScreenNeverBecomesAWallOfAlerts() {
        List<Signal> many = List.of(
                signal("أ", SignalKind.RISK, 900, 1),
                signal("ب", SignalKind.RISK, 800, 1),
                signal("ج", SignalKind.RISK, 700, 1),
                signal("د", SignalKind.RISK, 600, 1),
                signal("هـ", SignalKind.RISK, 500, 1),
                signal("و", SignalKind.RISK, 400, 1),
                signal("ز", SignalKind.RISK, 300, 1));

        assertThat(service.rank(many, 5)).hasSize(5);
    }

    @Test
    void theCapKeepsTheHighestImpactSignalsNotTheFirstSeen() {
        List<Signal> ranked = service.rank(List.of(
                signal("صغير", SignalKind.RISK, 100, 1),
                signal("كبير", SignalKind.RISK, 9_000, 1)), 1);

        assertThat(ranked).extracting(Signal::title).containsExactly("كبير");
    }

    @Test
    void anEmptyFeedIsReturnedRatherThanFailing() {
        assertThat(service.rank(List.of(), 5)).isEmpty();
    }

    @Test
    void zeroImpactSignalsStillRankBelowRealOnesInsteadOfBeingDropped() {
        List<Signal> ranked = service.rank(List.of(
                signal("بلا أثر مُقدَّر", SignalKind.OPPORTUNITY, 0, 1),
                signal("له أثر", SignalKind.RISK, 50, 1)), 5);

        assertThat(ranked).extracting(Signal::title).containsExactly("له أثر", "بلا أثر مُقدَّر");
    }

    // ---------- الإشارات المشتقة ----------

    @Test
    void stagnantStockBecomesAnOpportunityToFreeTiedUpCapital() {
        Signal s = service.stagnantStockSignal("جبنة بيضاء", 3_400, 12L);

        assertThat(s.kind()).isEqualTo(SignalKind.OPPORTUNITY);
        assertThat(s.expectedImpact()).isEqualTo(3_400);
        assertThat(s.title()).contains("جبنة بيضاء");
        assertThat(s.itemId()).isEqualTo(12L);
    }

    @Test
    void nearExpiryStockIsARiskAndGrowsMoreUrgentAsTheDateApproaches() {
        Signal soon = service.expirySignal("حليب", 500, 2, 3L);
        Signal later = service.expirySignal("حليب", 500, 25, 3L);

        assertThat(soon.kind()).isEqualTo(SignalKind.RISK);
        assertThat(soon.urgency()).isGreaterThan(later.urgency());
    }

    /**
     * مخزون راكد أو قارب على الانتهاء يمكن تحويله إلى طلب جماعي - وهو المسار الذي
     * يكسب منه النظام. الإشارة تحمل ذلك صراحةً بدل تركه للمستخدم.
     */
    @Test
    void aStagnantSignalPointsAtTheGroupOrderRouteAsItsSuggestedAction() {
        Signal s = service.stagnantStockSignal("جبنة بيضاء", 3_400, 12L);

        assertThat(s.suggestedAction()).isNotBlank();
    }
}
