package com.trust.service;

import com.trust.domain.*;
import com.trust.repository.*;
import com.trust.web.dto.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    /** سقف الشاشة الرئيسية - رؤية المنتج: خمسون تنبيهًا تعني أن المستخدم يغلق التطبيق */
    private static final int MAX_HOME_SCREEN_SIGNALS = 5;
    /** بضاعة تنتهي خلال هذه المدة تُعتبر إشارة تستحق الظهور */
    private static final int EXPIRY_HORIZON_DAYS = 30;

    private final BranchRepository branchRepository;
    private final DailyEntryRepository dailyEntryRepository;
    private final ItemRepository itemRepository;
    private final RecommendationRepository recommendationRepository;
    private final BhiService bhiService;
    private final OpportunityFeedService opportunityFeedService;
    private final ItemService itemService;
    private final DecisionRepository decisionRepository;
    private final GroupOrderParticipantRepository groupOrderParticipantRepository;
    private final DecisionAnalyticsService decisionAnalyticsService;
    private final HealthScoreHistoryRepository healthScoreHistoryRepository;
    private final GroupOrderRepository groupOrderRepository;

    public DashboardService(BranchRepository branchRepository, DailyEntryRepository dailyEntryRepository,
                             ItemRepository itemRepository, RecommendationRepository recommendationRepository,
                             BhiService bhiService, OpportunityFeedService opportunityFeedService,
                             ItemService itemService,
                             DecisionRepository decisionRepository,
                             GroupOrderParticipantRepository groupOrderParticipantRepository,
                             DecisionAnalyticsService decisionAnalyticsService,
                             HealthScoreHistoryRepository healthScoreHistoryRepository,
                             GroupOrderRepository groupOrderRepository) {
        this.branchRepository = branchRepository;
        this.dailyEntryRepository = dailyEntryRepository;
        this.itemRepository = itemRepository;
        this.recommendationRepository = recommendationRepository;
        this.bhiService = bhiService;
        this.opportunityFeedService = opportunityFeedService;
        this.itemService = itemService;
        this.decisionRepository = decisionRepository;
        this.groupOrderParticipantRepository = groupOrderParticipantRepository;
        this.decisionAnalyticsService = decisionAnalyticsService;
        this.healthScoreHistoryRepository = healthScoreHistoryRepository;
        this.groupOrderRepository = groupOrderRepository;
    }

    /**
     * @param branchId قد يكون null لعرض "جميع الفروع" (تجميع كل فروع المؤسسة)
     */
    public DashboardResponse build(Long organizationId, Long branchId, LocalDate from, LocalDate to) {
        List<Branch> branches = branchId != null
                ? List.of(branchRepository.findById(branchId).orElseThrow())
                : branchRepository.findByOrganizationId(organizationId);
        List<Long> branchIds = branches.stream().map(Branch::getId).toList();

        List<DailyEntry> entries = dailyEntryRepository
                .findByBranchIdInAndEntryDateBetweenOrderByEntryDateAsc(branchIds, from, to);
        List<Item> items = itemRepository.findByBranchIdIn(branchIds);

        double salesToday = latestValue(entries, DailyEntry::getTotalSales);
        double salesChange = changePercent(entries, DailyEntry::getTotalSales);
        double totalProfit = entries.stream().mapToDouble(DailyEntry::getTotalProfit).sum();
        double profitChange = changePercent(entries, DailyEntry::getTotalProfit);
        double marginPercent = salesSum(entries) > 0 ? (totalProfit / salesSum(entries)) * 100.0 : 0;
        double marginChange = 0; // مقارنة بسيطة يمكن تفعيلها لاحقًا بمقارنة فترتين
        double availableLiquidity = entries.isEmpty() ? 0 : entries.get(entries.size() - 1).getAvailableLiquidity();
        double liquidityChange = changePercent(entries, DailyEntry::getAvailableLiquidity);

        BhiResultDto healthScore = branches.size() == 1
                ? bhiService.calculate(branches.get(0), from, to)
                : bhiService.averageAcross(branches, from, to);

        List<DashboardResponse.SalesPoint> salesTrend = entries.stream()
                .map(e -> new DashboardResponse.SalesPoint(e.getEntryDate().toString(), e.getTotalSales()))
                .toList();

        List<Recommendation> recs = recommendationRepository
                .findByBranchIdInAndStatusOrderByExpectedValueDesc(branchIds, Recommendation.Status.OPEN);
        List<RecommendationDto> topRecs = recs.stream().limit(4)
                .map(r -> new RecommendationDto(r.getId(), r.getType().name(), r.getPriority().name(),
                        r.getTitle(), r.getExpectedValue(), r.getStatus().name()))
                .toList();

        Map<String, Double> inventoryBreakdown = new LinkedHashMap<>();
        for (Item.MovementStatus status : Item.MovementStatus.values()) {
            double value = items.stream().filter(i -> i.getMovementStatus() == status)
                    .mapToDouble(Item::getInventoryValue).sum();
            inventoryBreakdown.put(status.name(), value);
        }

        Map<String, Double> liquidityBreakdown = new LinkedHashMap<>();
        DailyEntry latest = entries.isEmpty() ? null : entries.get(entries.size() - 1);
        liquidityBreakdown.put("AVAILABLE", latest != null ? latest.getAvailableLiquidity() : 0);
        liquidityBreakdown.put("RECEIVABLES", latest != null ? latest.getReceivables() : 0);
        liquidityBreakdown.put("PAYABLES", latest != null ? latest.getPayables() : 0);

        List<ItemDto> attention = branchId != null
                ? itemService.listNeedingAttention(branchId)
                : branchIds.stream().flatMap(id -> itemService.listNeedingAttention(id).stream()).limit(10).toList();

        DailyPerformanceSummaryDto dailyPerformanceSummary =
                buildDailyPerformanceSummary(organizationId, branchIds, items, healthScore);
        PerformanceImpactSummaryDto performanceImpactSummary = decisionAnalyticsService.performanceImpactSummary(branchIds);
        MonthlyImpactLedgerDto monthlyImpactLedger = buildMonthlyImpactLedger(branchIds, dailyPerformanceSummary);
        ExecutiveActionCenterDto executiveActionCenter = buildExecutiveActionCenter(items, branchIds);

        return new DashboardResponse(salesToday, salesChange, totalProfit, profitChange, marginPercent, marginChange,
                availableLiquidity, liquidityChange, healthScore, salesTrend, topRecs,
                inventoryBreakdown, liquidityBreakdown, attention, dailyPerformanceSummary, performanceImpactSummary,
                monthlyImpactLedger, executiveActionCenter);
    }

    /** أعلى الأصناف تأثيرًا + تنبيهات تنفيذية حقيقية (لا توجد "عروض موردين" مُصطنعة - لا يوجد نموذج بيانات لها بعد) */
    private ExecutiveActionCenterDto buildExecutiveActionCenter(List<Item> items, List<Long> branchIds) {
        double branchDailyCogs = dailyEntryRepository
                .findByBranchIdInAndEntryDateBetweenOrderByEntryDateAsc(branchIds, LocalDate.now().minusDays(1), LocalDate.now())
                .stream().mapToDouble(DailyEntry::getTotalCogs).max().orElse(0);
        Map<Long, Double> dailySalesByItem = SalesEstimator.forBranch(items, branchDailyCogs);

        List<ExecutiveActionCenterDto.TopItemDto> topProfitability = items.stream()
                .filter(i -> i.getMovementStatus() != Item.MovementStatus.STAGNANT)
                .map(i -> new ExecutiveActionCenterDto.TopItemDto(i.getName(),
                        dailySalesByItem.getOrDefault(i.getId(), 0.0) * 30 * i.getSalePrice() * (i.getMarginPercent() / 100.0)))
                .sorted((a, b) -> Double.compare(b.value(), a.value()))
                .limit(3)
                .toList();

        List<ExecutiveActionCenterDto.TopItemDto> topAccumulatedCost = items.stream()
                .filter(i -> i.getMovementStatus() == Item.MovementStatus.SLOW || i.getMovementStatus() == Item.MovementStatus.STAGNANT)
                .map(i -> new ExecutiveActionCenterDto.TopItemDto(i.getName(), i.getInventoryValue()))
                .sorted((a, b) -> Double.compare(b.value(), a.value()))
                .limit(3)
                .toList();

        int openGroupOrders = groupOrderRepository.findByStatusOrderByCreatedAtDesc(GroupOrder.Status.COLLECTING).size();
        long lowStockAlerts = decisionRepository.findByBranchIdIn(branchIds).stream()
                .filter(d -> d.getStatus() == Decision.Status.OPEN && d.getCategory() == Decision.Category.RISK)
                .count();
        long slowMovingItems = items.stream()
                .filter(i -> i.getMovementStatus() == Item.MovementStatus.SLOW || i.getMovementStatus() == Item.MovementStatus.STAGNANT)
                .count();

        List<ExecutiveActionCenterDto.OpportunitySignalDto> todaysOpportunities =
                buildTodaysOpportunities(items, branchIds);

        List<ExecutiveActionCenterDto.ExecutiveAlertDto> alerts = List.of(
                new ExecutiveActionCenterDto.ExecutiveAlertDto("GROUP_ORDER", "طلبات شراء جماعي متاحة", openGroupOrders),
                new ExecutiveActionCenterDto.ExecutiveAlertDto("LOW_STOCK", "تنبيه مخزون منخفض", (int) lowStockAlerts),
                new ExecutiveActionCenterDto.ExecutiveAlertDto("SLOW_MOVING", "أصناف بطيئة الحركة", (int) slowMovingItems)
        );

        return new ExecutiveActionCenterDto(topProfitability, topAccumulatedCost, alerts, todaysOpportunities);
    }

    /** دفتر الأثر الشهري (Monthly Impact Ledger) - رؤية PM: "العميل يرى العائد، لا الفاتورة" */
    private MonthlyImpactLedgerDto buildMonthlyImpactLedger(List<Long> branchIds, DailyPerformanceSummaryDto dailySummary) {
        double[] riskAndOpportunity = decisionAnalyticsService.monthlyRiskAndOpportunityImpact(branchIds);
        double purchaseCostSavings = dailySummary.groupBuySavingsAmountThisMonth();
        double inventoryRiskImpact = riskAndOpportunity[0];
        double operatingProfitImpact = riskAndOpportunity[1];
        double total = purchaseCostSavings + inventoryRiskImpact + operatingProfitImpact;

        LocalDate trendFrom = LocalDate.now().minusMonths(6);
        List<HealthScoreHistory> history = healthScoreHistoryRepository
                .findByBranchIdInAndScoreDateBetweenOrderByScoreDateAsc(branchIds, trendFrom, LocalDate.now());
        Map<LocalDate, List<HealthScoreHistory>> byDate = new LinkedHashMap<>();
        for (HealthScoreHistory h : history) {
            byDate.computeIfAbsent(h.getScoreDate(), d -> new java.util.ArrayList<>()).add(h);
        }
        List<MonthlyImpactLedgerDto.TrendPoint> trend = byDate.entrySet().stream()
                .map(e -> new MonthlyImpactLedgerDto.TrendPoint(e.getKey().toString(),
                        Math.round(e.getValue().stream().mapToDouble(HealthScoreHistory::getTotalScore).average().orElse(0) * 10) / 10.0))
                .toList();

        return new MonthlyImpactLedgerDto(purchaseCostSavings, inventoryRiskImpact, operatingProfitImpact, total, trend);
    }

    /** ملخص الأداء اليومي (رؤية PM: الفرص/المخاطر مباشرة على الشاشة الرئيسية بدل أرقام مجردة) */
    private DailyPerformanceSummaryDto buildDailyPerformanceSummary(Long organizationId, List<Long> branchIds,
                                                                      List<Item> items, BhiResultDto healthScore) {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        double marketValueThisMonth = 0;
        double savingsThisMonth = 0;
        for (GroupOrderParticipant p : groupOrderParticipantRepository.findByOrganizationIdOrderByIdDesc(organizationId)) {
            GroupOrder order = p.getGroupOrder();
            if (order.getNegotiatedPrice() == null || order.getCreatedAt().isBefore(startOfMonth)) continue;
            marketValueThisMonth += order.getEstimatedMarketPrice() * p.getQuantity();
            savingsThisMonth += (order.getEstimatedMarketPrice() - order.getNegotiatedPrice()) * p.getQuantity();
        }
        double savingsRate = marketValueThisMonth > 0 ? Math.round(savingsThisMonth / marketValueThisMonth * 1000) / 10.0 : 0;

        double purchaseVolumeNeeded = decisionRepository.findByBranchIdIn(branchIds).stream()
                .filter(d -> d.getType() == Decision.Type.PURCHASE_ORDER)
                .filter(d -> d.getStatus() == Decision.Status.OPEN || d.getStatus() == Decision.Status.APPROVED || d.getStatus() == Decision.Status.MODIFIED)
                .mapToDouble(d -> (d.getApprovedQuantity() != null ? d.getApprovedQuantity() : d.getSuggestedQuantity()) * d.getItem().getCostPrice())
                .sum();

        double clearanceVolumeNeeded = items.stream()
                .filter(i -> i.getMovementStatus() == Item.MovementStatus.SLOW || i.getMovementStatus() == Item.MovementStatus.STAGNANT)
                .mapToDouble(i -> i.getQuantity() * i.getSalePrice())
                .sum();

        return new DailyPerformanceSummaryDto(savingsRate, Math.round(savingsThisMonth * 100) / 100.0,
                axisScoreOrZero(healthScore, BhiAxis.INVENTORY_MANAGEMENT), purchaseVolumeNeeded, clearanceVolumeNeeded);
    }

    private double salesSum(List<DailyEntry> entries) {
        return entries.stream().mapToDouble(DailyEntry::getTotalSales).sum();
    }

    private double latestValue(List<DailyEntry> entries, java.util.function.ToDoubleFunction<DailyEntry> extractor) {
        return entries.isEmpty() ? 0 : extractor.applyAsDouble(entries.get(entries.size() - 1));
    }

    private double changePercent(List<DailyEntry> entries, java.util.function.ToDoubleFunction<DailyEntry> extractor) {
        if (entries.size() < 2) return 0;
        double current = extractor.applyAsDouble(entries.get(entries.size() - 1));
        double previous = extractor.applyAsDouble(entries.get(entries.size() - 2));
        if (previous == 0) return 0;
        return Math.round(((current - previous) / previous) * 1000.0) / 10.0;
    }

    /** درجة محور واحد من نتيجة BHI - صفر إن كان المحور غير متاح لنقص بياناته */
    private double axisScoreOrZero(BhiResultDto bhi, BhiAxis axis) {
        return bhi.axes().stream()
                .filter(a -> a.axis() == axis && a.score() != null)
                .mapToDouble(BhiResultDto.AxisScore::score)
                .findFirst().orElse(0);
    }

    /**
     * يجمع الإشارات الموجودة أصلًا في طابور واحد مرتَّب: قرارات الشراء المفتوحة،
     * رأس المال المجمَّد في الأصناف الراكدة، والبضاعة التي تقترب من انتهاء صلاحيتها.
     * ليس محرك تحليل جديدًا - ترتيب فوق ما تحسبه المحركات القائمة.
     */
    private List<ExecutiveActionCenterDto.OpportunitySignalDto> buildTodaysOpportunities(
            List<Item> items, List<Long> branchIds) {

        List<OpportunityFeedService.Signal> signals = new ArrayList<>();

        for (Decision d : decisionRepository.findByBranchIdIn(branchIds)) {
            if (d.getStatus() != Decision.Status.OPEN) continue;
            signals.add(opportunityFeedService.purchaseDecisionSignal(
                    d.getItem().getName(), d.getFinancialImpact(),
                    d.getCategory() == Decision.Category.RISK, d.getItem().getId()));
        }

        for (Item i : items) {
            if (i.getMovementStatus() == Item.MovementStatus.STAGNANT && i.getInventoryValue() > 0) {
                signals.add(opportunityFeedService.stagnantStockSignal(
                        i.getName(), i.getInventoryValue(), i.getId()));
            }
            if (i.getExpiryDate() != null) {
                long days = ChronoUnit.DAYS.between(LocalDate.now(), i.getExpiryDate());
                if (days >= 0 && days <= EXPIRY_HORIZON_DAYS) {
                    signals.add(opportunityFeedService.expirySignal(
                            i.getName(), i.getInventoryValue(), (int) days, i.getId()));
                }
            }
        }

        return opportunityFeedService.rank(signals, MAX_HOME_SCREEN_SIGNALS).stream()
                .map(sig -> new ExecutiveActionCenterDto.OpportunitySignalDto(
                        sig.kind().name(), sig.title(), sig.detail(),
                        sig.expectedImpact(), sig.suggestedAction(), sig.itemId()))
                .toList();
    }
}
