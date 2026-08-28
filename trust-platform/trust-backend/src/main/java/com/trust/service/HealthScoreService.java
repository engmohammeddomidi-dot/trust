package com.trust.service;

import com.trust.domain.*;
import com.trust.repository.*;
import com.trust.web.dto.BhiResultDto;
import com.trust.web.dto.HealthScoreDto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * خدمة حساب مؤشر صحة الأعمال (Business Health Score).
 * تطبّق الصيغ الست الموصوفة في خطة MVP (القسم 4 من الخطة).
 */
@Service
public class HealthScoreService {

    private final ItemRepository itemRepository;
    private final DailyEntryRepository dailyEntryRepository;
    private final CategoryBenchmarkRepository benchmarkRepository;
    private final HealthScoreHistoryRepository historyRepository;
    private final BhiService bhiService;

    public HealthScoreService(ItemRepository itemRepository,
                               DailyEntryRepository dailyEntryRepository,
                               CategoryBenchmarkRepository benchmarkRepository,
                               HealthScoreHistoryRepository historyRepository,
                               BhiService bhiService) {
        this.itemRepository = itemRepository;
        this.dailyEntryRepository = dailyEntryRepository;
        this.benchmarkRepository = benchmarkRepository;
        this.historyRepository = historyRepository;
        this.bhiService = bhiService;
    }

    /**
     * يحفظ لقطة يومية من مؤشر صحة الأعمال حتى تتراكم بيانات تاريخية حقيقية لرسم اتجاه
     * الأداء بدل بيانات وهمية.
     *
     * منذ اعتماد نموذج BHI المرجعي، القيمة المحفوظة هي المؤشر العام لذلك النموذج - حتى
     * لا يوجد رقمان مختلفان يحملان اسم "مؤشر صحة الأعمال" في المنصة. أعمدة المحاور الستة
     * القديمة في هذا الجدول لم تُقرأ قط من أي مكان (كانت للكتابة فقط)، فتُركت كما هي.
     * ملاحظة صريحة: منحنى الاتجاه سيُظهر قفزة لمرة واحدة عند نقطة التحوّل، لأن المنهجيتين
     * لا تتفقان - وهذا مقبول لأن السلسلة عمرها أسابيع قليلة فقط.
     */
    public void snapshotToday(Branch branch) {
        LocalDate today = LocalDate.now();
        BhiResultDto bhi = bhiService.calculate(branch, today.minusDays(30), today);
        if (bhi.totalScore() == null) return; // بيانات غير كافية - لا نحفظ رقمًا مختلقًا

        HealthScoreHistory snapshot = historyRepository.findByBranchIdAndScoreDate(branch.getId(), today)
                .orElseGet(() -> {
                    HealthScoreHistory h = new HealthScoreHistory();
                    h.setBranch(branch);
                    h.setScoreDate(today);
                    return h;
                });
        snapshot.setTotalScore(round(bhi.totalScore()));
        historyRepository.save(snapshot);
    }

    public HealthScoreDto calculate(Branch branch, LocalDate from, LocalDate to) {
        Category category = branch.getOrganization().getCategory();
        CategoryBenchmark bm = benchmarkRepository.findById(category)
                .orElseGet(() -> defaultBenchmark(category));

        List<DailyEntry> entries = dailyEntryRepository
                .findByBranchIdAndEntryDateBetweenOrderByEntryDateAsc(branch.getId(), from, to);
        List<Item> items = itemRepository.findByBranchId(branch.getId());

        double salesScore = calculateSalesScore(entries);
        double profitScore = calculateProfitScore(entries, bm);
        double pricingScore = calculatePricingScore(items, bm);
        double purchasesScore = calculatePurchasesScore(items, entries, bm);
        double inventoryScore = calculateInventoryScore(items);
        double liquidityScore = calculateLiquidityScore(entries, bm);

        double total = salesScore * pct(bm.getWeightSales())
                + profitScore * pct(bm.getWeightProfit())
                + pricingScore * pct(bm.getWeightPricing())
                + purchasesScore * pct(bm.getWeightPurchases())
                + inventoryScore * pct(bm.getWeightInventory())
                + liquidityScore * pct(bm.getWeightLiquidity());

        return new HealthScoreDto(
                round(salesScore), round(profitScore), round(pricingScore),
                round(purchasesScore), round(inventoryScore), round(liquidityScore),
                round(total), label(total)
        );
    }

    /** نمو المبيعات مقابل الفترة السابقة بنفس الطول */
    private double calculateSalesScore(List<DailyEntry> entries) {
        if (entries.size() < 2) return 50; // لا يوجد بيانات كافية -> محايد
        int mid = entries.size() / 2;
        double firstHalf = entries.subList(0, mid).stream().mapToDouble(DailyEntry::getTotalSales).sum();
        double secondHalf = entries.subList(mid, entries.size()).stream().mapToDouble(DailyEntry::getTotalSales).sum();
        if (firstHalf <= 0) return secondHalf > 0 ? 100 : 50;
        double growth = (secondHalf - firstHalf) / firstHalf;
        return clamp(50 + growth * 200);
    }

    private double calculateProfitScore(List<DailyEntry> entries, CategoryBenchmark bm) {
        if (entries.isEmpty()) return 50;
        double totalSales = entries.stream().mapToDouble(DailyEntry::getTotalSales).sum();
        double totalProfit = entries.stream().mapToDouble(DailyEntry::getTotalProfit).sum();
        if (totalSales <= 0) return 50;
        double actualMargin = (totalProfit / totalSales) * 100.0;
        if (bm.getTargetMarginPercent() <= 0) return 50;
        return clamp((actualMargin / bm.getTargetMarginPercent()) * 100.0);
    }

    private double calculatePricingScore(List<Item> items, CategoryBenchmark bm) {
        if (items.isEmpty()) return 50;
        double low = bm.getTargetMarginPercent() - 5.0;
        double high = bm.getTargetMarginPercent() + 10.0;
        long inRange = items.stream()
                .filter(i -> i.getMarginPercent() >= low && i.getMarginPercent() <= high)
                .count();
        return clamp((inRange * 100.0) / items.size());
    }

    private double calculatePurchasesScore(List<Item> items, List<DailyEntry> entries, CategoryBenchmark bm) {
        if (items.isEmpty() || entries.isEmpty()) return 50;
        double inventoryValue = items.stream().mapToDouble(Item::getInventoryValue).sum();
        double totalSales = entries.stream().mapToDouble(DailyEntry::getTotalSales).sum();
        long daysSpan = Math.max(1, entries.size());
        double avgMonthlySales = (totalSales / daysSpan) * 30.0;
        if (avgMonthlySales <= 0) return 50;
        double coverageMonths = inventoryValue / avgMonthlySales;

        if (coverageMonths >= bm.getInventoryCoverageMinMonths() && coverageMonths <= bm.getInventoryCoverageMaxMonths()) {
            return 100;
        }
        double mid = (bm.getInventoryCoverageMinMonths() + bm.getInventoryCoverageMaxMonths()) / 2.0;
        double deviation = Math.abs(coverageMonths - mid) / Math.max(mid, 0.01);
        return clamp(100 - deviation * 60);
    }

    private double calculateInventoryScore(List<Item> items) {
        if (items.isEmpty()) return 50;
        double total = items.stream().mapToDouble(Item::getInventoryValue).sum();
        if (total <= 0) return 50;
        double weighted = items.stream().mapToDouble(i -> {
            double weight = switch (i.getMovementStatus()) {
                case FAST -> 1.0;
                case MEDIUM -> 0.6;
                case SLOW -> 0.3;
                case STAGNANT -> 0.0;
            };
            return i.getInventoryValue() * weight;
        }).sum();
        return clamp((weighted / total) * 100.0);
    }

    private double calculateLiquidityScore(List<DailyEntry> entries, CategoryBenchmark bm) {
        if (entries.isEmpty()) return 50;
        DailyEntry latest = entries.get(entries.size() - 1);
        if (latest.getPayables() <= 0) return latest.getAvailableLiquidity() > 0 ? 100 : 50;
        double ratio = latest.getAvailableLiquidity() / latest.getPayables();
        if (ratio >= bm.getLiquidityRatioMin() && ratio <= bm.getLiquidityRatioMax()) return 100;
        if (ratio < bm.getLiquidityRatioMin()) {
            double deficit = (bm.getLiquidityRatioMin() - ratio) / bm.getLiquidityRatioMin();
            return clamp(100 - deficit * 150); // عقوبة أشد لنقص السيولة
        }
        double excess = (ratio - bm.getLiquidityRatioMax()) / bm.getLiquidityRatioMax();
        return clamp(100 - excess * 40); // عقوبة أخف لسيولة زائدة مجمدة
    }

    private String label(double score) {
        if (score >= 81) return "ممتاز";
        if (score >= 61) return "جيد";
        if (score >= 41) return "مقبول";
        return "ضعيف";
    }

    private double pct(double weight) {
        return weight / 100.0;
    }

    private double clamp(double v) {
        return Math.max(0, Math.min(100, v));
    }

    private double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private CategoryBenchmark defaultBenchmark(Category category) {
        CategoryBenchmark bm = new CategoryBenchmark();
        bm.setCategory(category);
        bm.setTargetMarginPercent(category == Category.PHARMACY ? 28 : category == Category.RESTAURANT ? 35 : 20);
        bm.setLiquidityRatioMin(1.0);
        bm.setLiquidityRatioMax(2.0);
        bm.setInventoryCoverageMinMonths(1.0);
        bm.setInventoryCoverageMaxMonths(2.0);
        return bm;
    }
}
