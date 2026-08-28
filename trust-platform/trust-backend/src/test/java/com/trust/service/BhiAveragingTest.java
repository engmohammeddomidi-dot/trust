package com.trust.service;

import com.trust.domain.BhiAxis;
import com.trust.web.dto.BhiResultDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * دمج نتائج BHI لعدة فروع في رقم واحد على مستوى المؤسسة.
 *
 * التفاصيل على مستوى المؤشر لا تُدمَج عمدًا: متوسط "أيام تحصيل الذمم" لفرعين رقم بلا
 * معنى تشغيلي، والتفصيل متاح لكل فرع على حدة من مسار /api/bhi.
 */
class BhiAveragingTest {

    private final BhiScoringEngine engine = new BhiScoringEngine();

    private BhiResultDto branchResult(double total, double profitability, double liquidity) {
        return new BhiResultDto(total, engine.classify(total), 7, 13, List.of(
                new BhiResultDto.AxisScore(BhiAxis.PROFITABILITY, BhiAxis.PROFITABILITY.getLabelAr(),
                        profitability, 0.30, List.of()),
                new BhiResultDto.AxisScore(BhiAxis.LIQUIDITY, BhiAxis.LIQUIDITY.getLabelAr(),
                        liquidity, 0.20, List.of())));
    }

    @Test
    void overallScore_isTheMeanOfTheBranchScores() {
        BhiResultDto merged = engine.average(List.of(
                branchResult(80, 90, 70),
                branchResult(60, 70, 50)));

        assertThat(merged.totalScore()).isCloseTo(70.0, within(1e-9));
    }

    @Test
    void eachAxis_isAveragedAcrossTheBranchesThatReportIt() {
        BhiResultDto merged = engine.average(List.of(
                branchResult(80, 90, 70),
                branchResult(60, 70, 50)));

        assertThat(merged.axes()).hasSize(2);
        assertThat(merged.axes().get(0).score()).isCloseTo(80.0, within(1e-9)); // (90+70)/2
        assertThat(merged.axes().get(1).score()).isCloseTo(60.0, within(1e-9)); // (70+50)/2
    }

    @Test
    void mergedLabel_isDerivedFromTheMergedScoreNotCopiedFromABranch() {
        BhiResultDto merged = engine.average(List.of(
                branchResult(95, 95, 95),   // ممتازة
                branchResult(45, 45, 45))); // ضعيفة

        assertThat(merged.totalScore()).isCloseTo(70.0, within(1e-9));
        assertThat(merged.label()).isEqualTo("جيدة");
    }

    @Test
    void indicatorDetail_isNotMergedBecauseAveragingRawValuesAcrossBranchesIsMeaningless() {
        BhiResultDto merged = engine.average(List.of(
                branchResult(80, 90, 70),
                branchResult(60, 70, 50)));

        assertThat(merged.axes()).allSatisfy(a -> assertThat(a.indicators()).isEmpty());
    }

    @Test
    void branchesWithInsufficientData_areExcludedRatherThanCountedAsZero() {
        BhiResultDto insufficient = new BhiResultDto(null, "غير كافٍ", 0, 13, List.of());

        BhiResultDto merged = engine.average(List.of(branchResult(80, 90, 70), insufficient));

        assertThat(merged.totalScore()).isCloseTo(80.0, within(1e-9));
    }

    @Test
    void allBranchesInsufficient_yieldsUnknownRatherThanZero() {
        BhiResultDto insufficient = new BhiResultDto(null, "غير كافٍ", 0, 13, List.of());

        BhiResultDto merged = engine.average(List.of(insufficient, insufficient));

        assertThat(merged.totalScore()).isNull();
        assertThat(merged.label()).isEqualTo("غير كافٍ");
    }

    @Test
    void emptyBranchList_yieldsUnknownRatherThanThrowing() {
        BhiResultDto merged = engine.average(List.of());

        assertThat(merged.totalScore()).isNull();
        assertThat(merged.axes()).isEmpty();
    }
}
