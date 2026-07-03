package com.trust.service;

import com.trust.domain.*;
import com.trust.repository.CategoryBenchmarkRepository;
import com.trust.repository.ItemRepository;
import com.trust.repository.RecommendationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * محرك التوصيات القائم على قواعد ثابتة (بدون ذكاء اصطناعي) - القسم 6 من خطة MVP.
 * يُشغَّل دوريًا (يوميًا) لكل فرع وينتج توصيات جديدة بناءً على حالة المخزون والسيولة.
 *
 * كل استدعاء idempotent: إذا كانت هناك توصية مفتوحة بنفس (النوع + الصنف) من استدعاء
 * سابق، يُحدَّث نصها/قيمتها بدل إنشاء صف مكرر - ضروري لأن الجدولة اليومية (Scheduler)
 * تستدعي هذا تلقائيًا كل يوم لكل فرع.
 */
@Service
public class RecommendationEngineService {

    private static final double HIGH_PRIORITY_THRESHOLD = 50_000;
    private static final double MEDIUM_PRIORITY_THRESHOLD = 15_000;
    private static final int EXPIRY_WARNING_DAYS = 30;

    private final ItemRepository itemRepository;
    private final CategoryBenchmarkRepository benchmarkRepository;
    private final RecommendationRepository recommendationRepository;
    private final NotificationService notificationService;

    public RecommendationEngineService(ItemRepository itemRepository,
                                        CategoryBenchmarkRepository benchmarkRepository,
                                        RecommendationRepository recommendationRepository,
                                        NotificationService notificationService) {
        this.itemRepository = itemRepository;
        this.benchmarkRepository = benchmarkRepository;
        this.recommendationRepository = recommendationRepository;
        this.notificationService = notificationService;
    }

    public List<Recommendation> generateForBranch(Branch branch) {
        Category category = branch.getOrganization().getCategory();
        CategoryBenchmark bm = benchmarkRepository.findById(category).orElse(defaultBenchmark(category));
        List<Item> items = itemRepository.findByBranchId(branch.getId());

        Map<String, Recommendation> existingByKey = new HashMap<>();
        for (Recommendation r : recommendationRepository.findByBranchIdAndStatusOrderByExpectedValueDesc(branch.getId(), Recommendation.Status.OPEN)) {
            existingByKey.put(keyFor(r.getType(), r.getItem()), r);
        }
        java.util.Set<String> keysBeforeRun = new java.util.HashSet<>(existingByKey.keySet());

        List<Recommendation> results = new ArrayList<>();

        double targetMargin = bm.getTargetMarginPercent();
        double lowMargin = targetMargin - 5.0;
        double highMargin = targetMargin + 10.0;

        for (Item item : items) {
            // Rule 1: أوقف الشراء - مخزون راكد
            if (item.getMovementStatus() == Item.MovementStatus.STAGNANT) {
                double value = item.getInventoryValue();
                results.add(upsert(existingByKey, branch, item, Recommendation.Type.STOP_PURCHASE,
                        "أوقف شراء " + item.getName() + " — مخزون راكد يكلفك " + formatCurrency(value) + " شيكل",
                        value));
            }

            // Rule 2: زد الطلب - صنف سريع الحركة بهامش مرتفع وكمية منخفضة
            if (item.getMovementStatus() == Item.MovementStatus.FAST && item.getMarginPercent() > highMargin) {
                double dailyAvgSale = estimateDailySales(item);
                double daysRemaining = dailyAvgSale > 0 ? item.getQuantity() / dailyAvgSale : Double.MAX_VALUE;
                if (daysRemaining < 7) {
                    double opportunity = dailyAvgSale * (item.getMarginPercent() / 100.0) * item.getSalePrice() * 14;
                    results.add(upsert(existingByKey, branch, item, Recommendation.Type.INCREASE_ORDER,
                            "زد طلب " + item.getName() + " — فرصة لزيادة الربح " + formatCurrency(opportunity) + " شيكل",
                            opportunity));
                }
            }

            // Rule 3: عدّل السعر - هامش خارج النطاق الصحي
            if (item.getMarginPercent() < lowMargin || item.getMarginPercent() > highMargin) {
                double estimatedMonthlyUnits = Math.max(1, estimateDailySales(item) * 30);
                double marginGap = Math.abs(targetMargin - item.getMarginPercent()) / 100.0;
                double impact = estimatedMonthlyUnits * item.getSalePrice() * marginGap;
                results.add(upsert(existingByKey, branch, item, Recommendation.Type.ADJUST_PRICE,
                        "عدّل سعر " + item.getName() + " — تحسين الهامش وزيادة الربح " + formatCurrency(impact) + " شيكل",
                        impact));
            }

            // Rule 4: حملة ترويجية - صنف بطيء بقيمة مرتفعة
            if ((item.getMovementStatus() == Item.MovementStatus.SLOW
                    || item.getMovementStatus() == Item.MovementStatus.STAGNANT)
                    && item.getInventoryValue() > MEDIUM_PRIORITY_THRESHOLD / 2) {
                results.add(upsert(existingByKey, branch, item, Recommendation.Type.PROMOTION_CAMPAIGN,
                        "أطلق حملة ترويجية على " + item.getName() + " — تصفية مخزون بقيمة " + formatCurrency(item.getInventoryValue()) + " شيكل",
                        item.getInventoryValue() * 0.7));
            }

            // Rule 6: تنبيه صلاحية
            if (item.getExpiryDate() != null) {
                long daysToExpiry = LocalDate.now().until(item.getExpiryDate()).getDays();
                if (daysToExpiry >= 0 && daysToExpiry <= EXPIRY_WARNING_DAYS) {
                    results.add(upsert(existingByKey, branch, item, Recommendation.Type.EXPIRY_ALERT,
                            "صرّف " + item.getName() + " قبل انتهاء الصلاحية خلال " + daysToExpiry + " يوم",
                            item.getInventoryValue()));
                }
            }
        }

        long newHighPriorityCount = results.stream()
                .filter(r -> r.getPriority() == Recommendation.Priority.HIGH)
                .filter(r -> !keysBeforeRun.contains(keyFor(r.getType(), r.getItem())))
                .count();
        if (newHighPriorityCount > 0) {
            notificationService.notify(branch.getOrganization().getId(),
                    "توصيات جديدة عالية الأولوية",
                    "ظهرت " + newHighPriorityCount + " توصية جديدة عالية الأولوية لفرع " + branch.getName() + " — راجع صفحة التنبيهات",
                    com.trust.domain.Notification.Severity.WARNING);
        }

        return results;
    }

    /** Rule 5: تنبيه سيولة - يُستدعى من DashboardService بمعرفة آخر إدخال يومي */
    public Recommendation buildLiquidityAlert(Branch branch, double currentRatio, double payables) {
        return buildRecommendation(branch, null, Recommendation.Type.LIQUIDITY_ALERT,
                "تنبيه سيولة: نسبة التداول " + String.format("%.2f", currentRatio)
                        + " أقل من الحد الآمن — راجع خيارات تحرير السيولة",
                payables);
    }

    /** ينشئ توصية جديدة أو يحدّث توصية مفتوحة موجودة بنفس (النوع + الصنف) بدل تكرارها */
    private Recommendation upsert(Map<String, Recommendation> existingByKey, Branch branch, Item item,
                                   Recommendation.Type type, String title, double expectedValue) {
        String key = keyFor(type, item);
        Recommendation r = existingByKey.get(key);
        if (r == null) {
            r = buildRecommendation(branch, item, type, title, expectedValue);
            existingByKey.put(key, r);
        } else {
            r.setTitle(title);
            r.setExpectedValue(expectedValue);
            r.setPriority(priorityFor(expectedValue));
        }
        return r;
    }

    private String keyFor(Recommendation.Type type, Item item) {
        return type.name() + "|" + (item != null ? item.getId() : "null");
    }

    private Recommendation buildRecommendation(Branch branch, Item item, Recommendation.Type type,
                                                String title, double expectedValue) {
        Recommendation r = new Recommendation();
        r.setBranch(branch);
        r.setItem(item);
        r.setType(type);
        r.setTitle(title);
        r.setExpectedValue(expectedValue);
        r.setPriority(priorityFor(expectedValue));
        r.setStatus(Recommendation.Status.OPEN);
        return r;
    }

    public CategoryBenchmark resolveBenchmark(Category category) {
        return benchmarkRepository.findById(category).orElse(defaultBenchmark(category));
    }

    private Recommendation.Priority priorityFor(double expectedValue) {
        if (expectedValue >= HIGH_PRIORITY_THRESHOLD) return Recommendation.Priority.HIGH;
        if (expectedValue >= MEDIUM_PRIORITY_THRESHOLD) return Recommendation.Priority.MEDIUM;
        return Recommendation.Priority.LOW;
    }

    private double estimateDailySales(Item item) {
        return SalesEstimator.estimateDailySales(item);
    }

    private String formatCurrency(double value) {
        return String.format("%,.0f", value);
    }

    private CategoryBenchmark defaultBenchmark(Category category) {
        CategoryBenchmark bm = new CategoryBenchmark();
        bm.setCategory(category);
        bm.setTargetMarginPercent(20);
        bm.setLiquidityRatioMin(1.0);
        bm.setLiquidityRatioMax(2.0);
        return bm;
    }
}
