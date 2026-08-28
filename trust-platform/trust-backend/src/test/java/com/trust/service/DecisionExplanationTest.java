package com.trust.service;

import com.trust.domain.Decision;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * العناصر التي تجعل بطاقة القرار مقنعة، وفق دستور رؤية المنتج: كل توصية تجيب سؤالين
 * لا سؤالًا واحدًا - "ماذا يحدث إذا نفّذت؟" و"ماذا يحدث إذا تجاهلت؟" - وتعرض القيود
 * التي رُوعيت، وأسباب درجة الثقة بدل رقم مجرّد.
 *
 * كلها مشتقة من حسابات المحرك القائمة أصلًا، لا من بيانات جديدة.
 */
class DecisionExplanationTest {

    private final DecisionExplanationBuilder builder = new DecisionExplanationBuilder();

    @Test
    void ignoringTheDecision_isQuantifiedAsTheRevenueAtRisk() {
        String line = builder.ifIgnored(1_250.0, 3.5);

        assertThat(line).contains("1,250");
        assertThat(line).contains("3.5");
    }

    @Test
    void ifIgnoredLine_isStatedEvenWhenTheRiskWindowIsUnderADay() {
        String line = builder.ifIgnored(400.0, 0.4);

        assertThat(line).isNotBlank().contains("400");
    }

    // ---------- القيود ----------

    @Test
    void aLiquidityCappedOrder_saysSoAsAConstraint() {
        List<String> constraints = builder.constraints(true, false, 0.25, null);

        assertThat(constraints).anySatisfy(c -> assertThat(c).contains("السيولة"));
        assertThat(constraints).anySatisfy(c -> assertThat(c).contains("25"));
    }

    @Test
    void aSupplierBelowPolicy_isListedAsAConstraint() {
        List<String> constraints = builder.constraints(false, true, 0.25, "مؤسسة النور");

        assertThat(constraints).anySatisfy(c -> assertThat(c).contains("مؤسسة النور"));
    }

    /**
     * غياب القيود ليس فراغًا يُترك للمستخدم ليخمّنه - نقولها صراحةً، فذلك جزء من
     * إثبات أن المحرك فحص القيود أصلًا.
     */
    @Test
    void whenNothingConstrainedTheOrder_thatIsStatedExplicitly() {
        List<String> constraints = builder.constraints(false, false, 0.25, null);

        assertThat(constraints).hasSize(1);
        assertThat(constraints.get(0)).isNotBlank();
    }

    // ---------- أسباب الثقة ----------

    @Test
    void confidenceReasons_nameWhatRaisedAndWhatLoweredTheScore() {
        List<String> reasons = builder.confidenceReasons(true, true, false);

        assertThat(reasons).isNotEmpty();
        assertThat(String.join(" ", reasons)).contains("مورّد");
    }

    @Test
    void missingSupplier_isGivenAsAReasonConfidenceIsLower() {
        List<String> reasons = builder.confidenceReasons(false, true, false);

        assertThat(String.join(" ", reasons)).contains("لا يوجد مورّد");
    }

    @Test
    void missingLiquidityData_isGivenAsAReasonConfidenceIsLower() {
        List<String> reasons = builder.confidenceReasons(true, false, false);

        assertThat(String.join(" ", reasons)).contains("السيولة");
    }

    @Test
    void everyDecision_getsAtLeastOneConfidenceReasonSoTheScoreIsNeverBare() {
        for (boolean supplier : List.of(true, false)) {
            for (boolean liquidity : List.of(true, false)) {
                for (boolean belowPolicy : List.of(true, false)) {
                    assertThat(builder.confidenceReasons(supplier, liquidity, belowPolicy))
                            .as("supplier=%s liquidity=%s belowPolicy=%s", supplier, liquidity, belowPolicy)
                            .isNotEmpty();
                }
            }
        }
    }

    // ---------- تخزينها على القرار ----------

    @Test
    void theExplanationIsPersistedOnTheDecisionSoTheCardCanAlwaysShowIt() {
        Decision d = new Decision();
        builder.applyTo(d, 1_250.0, 3.5, true, false, 0.25, null, true, true);

        assertThat(d.getIfIgnoredSummary()).isNotBlank();
        assertThat(d.getConstraintsSummary()).isNotBlank();
        assertThat(d.getConfidenceReasons()).isNotBlank();
    }
}
