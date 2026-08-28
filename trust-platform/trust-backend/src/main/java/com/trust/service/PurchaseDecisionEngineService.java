package com.trust.service;

import com.trust.domain.Branch;
import com.trust.domain.DailyEntry;
import com.trust.domain.Decision;
import com.trust.domain.Goal;
import com.trust.domain.Item;
import com.trust.domain.Policy;
import com.trust.domain.Supplier;
import com.trust.repository.DailyEntryRepository;
import com.trust.repository.DecisionRepository;
import com.trust.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * محرك قرار "إصدار أمر شراء" - أول شريحة من محرك القرارات (Decision Engine) الموصوف
 * في رؤية PM: كل قرار يُفسَّر بالكامل (السبب، درجة الثقة، الأثر المالي) بدل توصية
 * مجردة، ويأخذ السيولة المتاحة بعين الاعتبار قبل اقتراح كمية الشراء.
 *
 * لا يستبدل RecommendationEngineService (لا يزال يغطي بقية القواعد الخمس)، بل
 * يضيف تحليلاً أعمق لقرار الشراء تحديدًا كنموذج مرجعي يُبنى عليه لاحقًا لأنواع
 * قرارات أخرى (تسعير، عروض، تبديل مورّد...).
 */
@Service
public class PurchaseDecisionEngineService {

    /** أيام تشغيل إضافية مستهدفة بعد تغطية مدة التوريد ومخزون الأمان، حتى لا يُعاد الطلب كل يوم */
    private static final int ORDER_CYCLE_BUFFER_DAYS = 7;

    /** مدة توريد افتراضية تُستخدم إذا لم يكن للصنف مورّد مرتبط بعد */
    private static final int DEFAULT_LEAD_TIME_DAYS = 5;

    /** إذا مضى على آخر بيع أكثر من هذه المدة، يُعتبر تقدير المبيعات اليومية أقل موثوقية */
    private static final int STALE_SALES_DATA_DAYS = 14;

    private final ItemRepository itemRepository;
    private final DailyEntryRepository dailyEntryRepository;
    private final DecisionRepository decisionRepository;
    private final PolicyService policyService;
    private final GoalService goalService;
    private final DecisionExplanationBuilder explanationBuilder;
    private final DecisionAlternativeBuilder alternativeBuilder;

    public PurchaseDecisionEngineService(ItemRepository itemRepository,
                                          DailyEntryRepository dailyEntryRepository,
                                          DecisionRepository decisionRepository,
                                          PolicyService policyService,
                                          GoalService goalService,
                                          DecisionExplanationBuilder explanationBuilder,
                                          DecisionAlternativeBuilder alternativeBuilder) {
        this.itemRepository = itemRepository;
        this.dailyEntryRepository = dailyEntryRepository;
        this.decisionRepository = decisionRepository;
        this.policyService = policyService;
        this.goalService = goalService;
        this.explanationBuilder = explanationBuilder;
        this.alternativeBuilder = alternativeBuilder;
    }

    public List<Decision> generateForBranch(Branch branch) {
        List<Item> items = itemRepository.findByBranchId(branch.getId());
        Optional<DailyEntry> latestEntry = dailyEntryRepository.findTopByBranchIdOrderByEntryDateDesc(branch.getId());
        Long organizationId = branch.getOrganization().getId();
        Policy policy = policyService.resolveForOrganization(organizationId);

        // تأثير طبقة الأهداف على المحرك: أولوية "منع نفاد المخزون" تزيد الحذر في إعادة الطلب،
        // وأولوية "تحسين السيولة" تُشدّد سقف الشراء المسموح به من السيولة المتاحة - القيمة
        // الافتراضية 3 (محايدة) لا تغيّر شيئًا.
        int preventStockoutsPriority = goalService.priorityOf(organizationId, Goal.Type.PREVENT_STOCKOUTS);
        int improveLiquidityPriority = goalService.priorityOf(organizationId, Goal.Type.IMPROVE_LIQUIDITY);
        int extraSafetyDaysFromGoal = preventStockoutsPriority - GoalService.DEFAULT_PRIORITY;
        double liquidityRatioMultiplier = 1.0 - 0.1 * (improveLiquidityPriority - GoalService.DEFAULT_PRIORITY);

        Map<Long, Decision> existingByItemId = new HashMap<>();
        for (Decision d : decisionRepository.findByBranchIdAndStatusOrderByFinancialImpactDesc(branch.getId(), Decision.Status.OPEN)) {
            if (d.getType() == Decision.Type.PURCHASE_ORDER) {
                existingByItemId.put(d.getItem().getId(), d);
            }
        }

        // تقدير مثبَّت على تكلفة البضاعة المباعة الفعلية بدل نسبة من المخزون - وإلا
        // بقيت أيام التغطية ثابتة مهما تغيّر المخزون
        double branchDailyCogs = latestEntry.map(DailyEntry::getTotalCogs).orElse(0.0);
        Map<Long, Double> dailySalesByItem = SalesEstimator.forBranch(items, branchDailyCogs);

        List<Decision> results = new ArrayList<>();
        for (Item item : items) {
            Decision decision = evaluateItem(branch, item, latestEntry, policy, extraSafetyDaysFromGoal,
                    liquidityRatioMultiplier, existingByItemId.get(item.getId()),
                    dailySalesByItem.getOrDefault(item.getId(), 0.0));
            if (decision != null) {
                results.add(decision);
            }
        }
        return results;
    }

    private Decision evaluateItem(Branch branch, Item item, Optional<DailyEntry> latestEntry, Policy policy,
                                   int extraSafetyDaysFromGoal, double liquidityRatioMultiplier, Decision existing,
                                   double dailyAvgSales) {
        if (item.getMovementStatus() == Item.MovementStatus.STAGNANT) {
            return null; // لا يُشترى صنف راكد أصلاً - السؤال "هل يجب أن أشتري أصلاً؟" إجابته لا هنا
        }

        if (dailyAvgSales <= 0) {
            return null;
        }

        Supplier supplier = item.getSupplier();
        boolean supplierBelowPolicy = supplier != null && supplier.getRating() < policy.getMinSupplierRating();
        int leadTimeDays = supplier != null ? supplier.getLeadTimeDays() : DEFAULT_LEAD_TIME_DAYS;
        int safetyStockDays = Math.max(0, item.getSafetyStockDays() + extraSafetyDaysFromGoal);
        double daysCoverage = item.getQuantity() / dailyAvgSales;
        int reorderThresholdDays = leadTimeDays + safetyStockDays;

        if (daysCoverage > reorderThresholdDays) {
            return null; // المخزون كافٍ حتى وصول طلبية جديدة - لا حاجة لقرار الآن
        }

        int targetCoverageDays = reorderThresholdDays + ORDER_CYCLE_BUFFER_DAYS;
        double suggestedQuantity = Math.max(1, Math.ceil(dailyAvgSales * targetCoverageDays - item.getQuantity()));

        boolean liquidityDataAvailable = latestEntry.isPresent();
        boolean liquidityCapped = false;
        double effectiveLiquidityRatio = Math.max(0.05, Math.min(1.0, policy.getMaxPurchaseLiquidityRatio() * liquidityRatioMultiplier));
        if (liquidityDataAvailable && item.getCostPrice() > 0) {
            double availableLiquidity = latestEntry.get().getAvailableLiquidity();
            double liquidityCap = availableLiquidity * effectiveLiquidityRatio;
            double orderValue = suggestedQuantity * item.getCostPrice();
            if (orderValue > liquidityCap) {
                double cappedQuantity = Math.floor(liquidityCap / item.getCostPrice());
                if (cappedQuantity < suggestedQuantity) {
                    suggestedQuantity = Math.max(0, cappedQuantity);
                    liquidityCapped = true;
                }
            }
        }

        double stockoutRiskDays = Math.max(1, reorderThresholdDays - daysCoverage);
        double financialImpact = dailyAvgSales * item.getSalePrice() * (item.getMarginPercent() / 100.0) * stockoutRiskDays;

        double confidence = 95.0;
        if (supplier == null) confidence -= 20;
        if (!liquidityDataAvailable) confidence -= 15;
        if (item.getLastSaleDate() == null
                || ChronoUnit.DAYS.between(item.getLastSaleDate(), LocalDate.now()) > STALE_SALES_DATA_DAYS) {
            confidence -= 10;
        }
        if (liquidityCapped) confidence -= 10;
        if (supplierBelowPolicy) confidence -= 25;
        confidence = Math.max(30, Math.min(99, confidence));

        String reason = buildReason(item, supplier, leadTimeDays, safetyStockDays, dailyAvgSales, daysCoverage,
                suggestedQuantity, targetCoverageDays, liquidityCapped, liquidityDataAvailable, supplierBelowPolicy,
                policy, effectiveLiquidityRatio, extraSafetyDaysFromGoal);

        // خطر: المخزون لن يصمد حتى مدة توريد المورد نفسها (احتمال نفاد حقيقي قبل وصول أي طلبية جديدة).
        // فرصة: القرار استباقي وما زال هناك هامش أمان - تحسين استباقي وليس إطفاء حريق.
        Decision.Category category = daysCoverage <= leadTimeDays ? Decision.Category.RISK : Decision.Category.OPPORTUNITY;

        double availableLiquidity = liquidityDataAvailable ? latestEntry.get().getAvailableLiquidity() : 0;
        String alternativesJson = serialiseAlternatives(alternativeBuilder.build(
                suggestedQuantity, item.getCostPrice(), availableLiquidity, effectiveLiquidityRatio,
                dailyAvgSales, reorderThresholdDays));

        if (existing != null) {
            existing.setSupplier(supplier);
            existing.setSuggestedQuantity(suggestedQuantity);
            existing.setReasonSummary(reason);
            existing.setConfidenceScore(confidence);
            existing.setFinancialImpact(financialImpact);
            existing.setCategory(category);
            existing.setAlternativesJson(alternativesJson);
            explanationBuilder.applyTo(existing, financialImpact, stockoutRiskDays, liquidityCapped,
                    supplierBelowPolicy, effectiveLiquidityRatio,
                    supplier != null ? supplier.getName() : null, supplier != null, liquidityDataAvailable);
            return existing;
        }

        Decision decision = new Decision();
        decision.setBranch(branch);
        decision.setItem(item);
        decision.setSupplier(supplier);
        decision.setType(Decision.Type.PURCHASE_ORDER);
        decision.setStatus(Decision.Status.OPEN);
        decision.setCategory(category);
        decision.setSuggestedQuantity(suggestedQuantity);
        decision.setReasonSummary(reason);
        decision.setConfidenceScore(confidence);
        decision.setFinancialImpact(financialImpact);
        decision.setAlternativesJson(alternativesJson);
        explanationBuilder.applyTo(decision, financialImpact, stockoutRiskDays, liquidityCapped,
                supplierBelowPolicy, effectiveLiquidityRatio,
                supplier != null ? supplier.getName() : null, supplier != null, liquidityDataAvailable);
        return decision;
    }

    /**
     * البدائل تُخزَّن كـ JSON على القرار بدل جدول منفصل: لا يُستعلَم عنها إلا مع القرار
     * نفسه، ولا تُصفّى ولا تُجمَّع - فجدول مستقل سيضيف كلفة بلا فائدة.
     */
    private String serialiseAlternatives(java.util.List<DecisionAlternativeBuilder.Alternative> alternatives) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(alternatives);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return null; // البدائل تحسين للعرض - فشل تسلسلها لا يُسقط توليد القرار
        }
    }

    private String buildReason(Item item, Supplier supplier, int leadTimeDays, int safetyStockDays,
                                double dailyAvgSales, double daysCoverage, double suggestedQuantity,
                                int targetCoverageDays, boolean liquidityCapped, boolean liquidityDataAvailable,
                                boolean supplierBelowPolicy, Policy policy, double effectiveLiquidityRatio,
                                int extraSafetyDaysFromGoal) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("المخزون الحالي يكفي حوالي %.0f يوم بمعدل بيع يومي %.1f وحدة. ", daysCoverage, dailyAvgSales));
        if (supplier != null) {
            sb.append(String.format("المورد %s يحتاج %d يوم للتوريد. ", supplier.getName(), leadTimeDays));
        } else {
            sb.append(String.format("لا يوجد مورد مرتبط بهذا الصنف بعد - تم افتراض مدة توريد %d يوم. ", leadTimeDays));
        }
        sb.append(String.format("مخزون الأمان المطلوب %d يوم", safetyStockDays));
        if (extraSafetyDaysFromGoal > 0) {
            sb.append(String.format(" (شمل %d يوم إضافي بسبب أولوية \"منع نفاد المخزون\")", extraSafetyDaysFromGoal));
        } else if (extraSafetyDaysFromGoal < 0) {
            sb.append(String.format(" (خُفّض %d يوم لأن أولوية \"منع نفاد المخزون\" منخفضة)", -extraSafetyDaysFromGoal));
        }
        sb.append(". ");
        if (supplierBelowPolicy) {
            sb.append(String.format("تنبيه: تقييم المورد %s (%.0f) أقل من الحد الأدنى الذي تحدده سياسة المؤسسة (%.0f) - يُنصح باختيار مورد آخر أو الموافقة يدويًا. ",
                    supplier.getName(), supplier.getRating(), policy.getMinSupplierRating()));
        }
        if (liquidityCapped) {
            sb.append(String.format("تم تخفيض الكمية المقترحة بسبب محدودية السيولة المتاحة حاليًا (الحد الآمن لطلبية واحدة %.0f%% من السيولة المتاحة%s). ",
                    effectiveLiquidityRatio * 100, effectiveLiquidityRatio < policy.getMaxPurchaseLiquidityRatio() ? " - مُشدَّد بسبب أولوية تحسين السيولة" : ""));
        } else if (!liquidityDataAvailable) {
            sb.append("لا تتوفر بيانات سيولة حديثة للتحقق من الأثر على التدفق النقدي. ");
        }
        sb.append(String.format("الكمية المقترحة: %.0f وحدة لتغطية حوالي %d يوم.", suggestedQuantity, targetCoverageDays));
        return sb.toString();
    }
}
