package com.trust.service;

import com.trust.domain.Item;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * تقدير المبيعات اليومية لكل صنف.
 *
 * التقدير القديم كان دائريًا: يفترض أن الصنف يبيع 15% من كميته يوميًا، فيصبح "أيام
 * التغطية" ثابتًا عند ~6.7 يوم مهما بلغ المخزون، ويصبح مجموع المبيعات المقدَّرة لكل
 * الأصناف بلا أي علاقة بمبيعات الفرع الحقيقية. عمليًا كان يقول إن 2,800 دجاجة تنفد
 * خلال 1.3 يوم في محل مبيعاته 3,500 شيكل يوميًا.
 *
 * البديل يُثبِّت المجموع على تكلفة البضاعة المباعة الفعلية من الإدخالات اليومية،
 * ويوزّعها على الأصناف بحسب قيمتها في المخزون مرجَّحةً بسرعة حركتها.
 */
class SalesEstimatorTest {

    private Item item(long id, String name, double cost, double qty, Item.MovementStatus status) {
        Item i = new Item();
        i.setId(id);
        i.setName(name);
        i.setCostPrice(cost);
        i.setSalePrice(cost * 1.3);
        i.setQuantity(qty);
        i.setMovementStatus(status);
        return i;
    }

    @Test
    void estimatedSalesAcrossItems_addUpToTheBranchesActualCostOfGoodsSold() {
        List<Item> items = List.of(
                item(1, "أ", 10, 500, Item.MovementStatus.FAST),
                item(2, "ب", 20, 200, Item.MovementStatus.MEDIUM),
                item(3, "ج", 5, 400, Item.MovementStatus.SLOW));

        Map<Long, Double> daily = SalesEstimator.forBranch(items, 2_730);

        double totalCogs = items.stream()
                .mapToDouble(i -> daily.get(i.getId()) * i.getCostPrice())
                .sum();
        assertThat(totalCogs).isCloseTo(2_730, within(0.01));
    }

    @Test
    void aFasterMovingItemIsEstimatedToSellMoreThanASlowerOne() {
        List<Item> items = List.of(
                item(1, "سريع", 10, 100, Item.MovementStatus.FAST),
                item(2, "بطيء", 10, 100, Item.MovementStatus.SLOW));

        Map<Long, Double> daily = SalesEstimator.forBranch(items, 1_000);

        assertThat(daily.get(1L)).isGreaterThan(daily.get(2L));
    }

    @Test
    void stagnantItemsAreEstimatedAtZeroBecauseTheyAreNotSelling() {
        List<Item> items = List.of(
                item(1, "متحرك", 10, 100, Item.MovementStatus.FAST),
                item(2, "راكد", 10, 100, Item.MovementStatus.STAGNANT));

        Map<Long, Double> daily = SalesEstimator.forBranch(items, 1_000);

        assertThat(daily.get(2L)).isZero();
    }

    /**
     * الجوهر: التغطية يجب أن تستجيب لمستوى المخزون.
     *
     * الاختبار بصنف واحد كان يمرّ حتى مع توزيع دائري، فهو يستخدم صنفين: صنف مكدَّس
     * وآخر شحيح من نفس فئة الحركة. لو وُزِّعت المبيعات بحسب قيمة المخزون لخرج الصنفان
     * بنفس أيام التغطية تمامًا - وهي الدائرية نفسها في ثوب جديد.
     */
    @Test
    void aWellStockedItemHasLongerCoverageThanAScarceOneOfTheSameMovementClass() {
        List<Item> items = List.of(
                item(1, "مكدَّس", 10, 1_000, Item.MovementStatus.FAST),
                item(2, "شحيح", 10, 20, Item.MovementStatus.FAST));

        Map<Long, Double> daily = SalesEstimator.forBranch(items, 1_000);

        double coverageStocked = 1_000 / daily.get(1L);
        double coverageScarce = 20 / daily.get(2L);

        assertThat(coverageStocked).isGreaterThan(coverageScarce * 10);
    }

    @Test
    void doublingStockDoublesCoverage() {
        List<Item> before = List.of(
                item(1, "أ", 10, 100, Item.MovementStatus.FAST),
                item(2, "ب", 10, 100, Item.MovementStatus.FAST));
        List<Item> after = List.of(
                item(1, "أ", 10, 200, Item.MovementStatus.FAST),
                item(2, "ب", 10, 100, Item.MovementStatus.FAST));

        double coverageBefore = 100 / SalesEstimator.forBranch(before, 1_000).get(1L);
        double coverageAfter = 200 / SalesEstimator.forBranch(after, 1_000).get(1L);

        assertThat(coverageAfter).isCloseTo(coverageBefore * 2, within(0.01));
    }

    // ---------- التراجع الآمن حين لا تتوفر بيانات مبيعات ----------

    @Test
    void withNoSalesData_theLegacyHeuristicIsUsedSoNothingBreaks() {
        List<Item> items = List.of(item(1, "أ", 10, 500, Item.MovementStatus.FAST));

        Map<Long, Double> daily = SalesEstimator.forBranch(items, 0);

        assertThat(daily.get(1L)).isCloseTo(500 * 0.15, within(0.001));
    }

    @Test
    void anItemWithoutCostPrice_isNotEstimatedRatherThanDividingByZero() {
        List<Item> items = List.of(
                item(1, "بلا تكلفة", 0, 100, Item.MovementStatus.FAST),
                item(2, "عادي", 10, 100, Item.MovementStatus.FAST));

        Map<Long, Double> daily = SalesEstimator.forBranch(items, 1_000);

        assertThat(daily.get(1L)).isZero();
        assertThat(daily.get(2L)).isGreaterThan(0);
    }

    @Test
    void anEmptyItemListYieldsAnEmptyMap() {
        assertThat(SalesEstimator.forBranch(List.of(), 1_000)).isEmpty();
    }

    @Test
    void everyItemHasAnEntrySoCallersNeverGetANullEstimate() {
        List<Item> items = List.of(
                item(1, "أ", 10, 100, Item.MovementStatus.FAST),
                item(2, "ب", 10, 100, Item.MovementStatus.STAGNANT));

        assertThat(SalesEstimator.forBranch(items, 1_000)).containsOnlyKeys(1L, 2L);
    }
}
