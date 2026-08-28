package com.trust.service;

import com.trust.service.DecisionAlternativeBuilder.Alternative;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * البدائل على بطاقة القرار - "لا توجد توصية دون بدائل" في رؤية المنتج.
 *
 * الشرط الحاسم: البديل يخضع لنفس سقف السيولة الذي تخضع له الكمية الموصى بها. بدون
 * ذلك يعرض النظام على صاحب المحل خيارًا تمنعه سياسة مؤسسته - وهو أسوأ من ألا يعرض
 * بدائل أصلًا.
 */
class DecisionAlternativesTest {

    private final DecisionAlternativeBuilder builder = new DecisionAlternativeBuilder();

    /** سقف واسع لا يقيّد شيئًا: 100,000 سيولة × 25% ÷ تكلفة 10 = 2,500 وحدة */
    private List<Alternative> generous(double recommendedQty) {
        return builder.build(recommendedQty, 10.0, 100_000, 0.25, 5.0, 8);
    }

    @Test
    void threeOptionsAreOffered_notASingleTakeItOrLeaveIt() {
        assertThat(generous(100)).hasSize(3);
    }

    @Test
    void exactlyOneOptionIsFlaggedAsRecommended() {
        assertThat(generous(100)).filteredOn(Alternative::recommended).hasSize(1);
    }

    @Test
    void theRecommendedOptionCarriesTheEngineQuantity() {
        Alternative recommended = generous(100).stream()
                .filter(Alternative::recommended).findFirst().orElseThrow();

        assertThat(recommended.quantity()).isCloseTo(100, within(0.001));
    }

    @Test
    void theOptionsSpanAConservativeAndAnExtendedChoiceAroundTheRecommendation() {
        List<Double> quantities = generous(100).stream().map(Alternative::quantity).toList();

        assertThat(quantities).hasSize(3);
        assertThat(quantities.get(0)).isLessThan(100);
        assertThat(quantities.get(2)).isGreaterThan(100);
    }

    @Test
    void everyOptionStatesItsCostAndTheCoverageItBuys() {
        for (Alternative a : generous(100)) {
            assertThat(a.orderValue()).isCloseTo(a.quantity() * 10.0, within(0.001));
            assertThat(a.coverageDays()).isGreaterThan(0);
            assertThat(a.label()).isNotBlank();
            assertThat(a.tradeOff()).isNotBlank();
        }
    }

    // ---------- سقف السيولة يُطبَّق على البدائل أيضًا ----------

    /**
     * سيولة 1,000 × 25% = 250 شيكل، أي 25 وحدة بتكلفة 10. البديل الأوسع (130 وحدة)
     * يجب أن يُقصَّ إلى 25، لا أن يُعرض كما هو.
     */
    @Test
    void anOptionExceedingTheLiquidityCap_isTrimmedToTheCap() {
        List<Alternative> options = builder.build(100, 10.0, 1_000, 0.25, 5.0, 8);

        assertThat(options).allSatisfy(a -> assertThat(a.quantity()).isLessThanOrEqualTo(25.0));
    }

    @Test
    void aTrimmedOptionSaysItWasLimitedByLiquidity() {
        List<Alternative> options = builder.build(100, 10.0, 1_000, 0.25, 5.0, 8);

        assertThat(options).anySatisfy(a -> assertThat(a.liquidityLimited()).isTrue());
    }

    @Test
    void optionsWithinTheCapAreNotMarkedAsLimited() {
        assertThat(generous(100)).allSatisfy(a -> assertThat(a.liquidityLimited()).isFalse());
    }

    /**
     * حين يقصّ السقف كل الخيارات إلى نفس الكمية، لا معنى لعرض ثلاثة خيارات متطابقة -
     * يُعرض خيار واحد صادق بدل ثلاثة يوهمون بوجود اختيار.
     */
    @Test
    void whenTheCapCollapsesEveryOptionToTheSameQuantity_onlyOneIsOffered() {
        List<Alternative> options = builder.build(100, 10.0, 100, 0.25, 5.0, 8);

        assertThat(options).hasSize(1);
        assertThat(options.get(0).recommended()).isTrue();
    }

    @Test
    void noLiquidityDataMeansNoCapIsApplied() {
        List<Alternative> options = builder.build(100, 10.0, 0, 0.25, 5.0, 8);

        assertThat(options).hasSize(3);
        assertThat(options).allSatisfy(a -> assertThat(a.liquidityLimited()).isFalse());
    }

    @Test
    void quantitiesAreWholeUnitsBecauseYouCannotOrderAFractionOfAnItem() {
        for (Alternative a : generous(37)) {
            assertThat(a.quantity()).isEqualTo(Math.floor(a.quantity()));
        }
    }
}
