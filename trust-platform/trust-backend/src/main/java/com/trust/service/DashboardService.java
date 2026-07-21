package com.trust.service;

import com.trust.domain.*;
import com.trust.repository.*;
import com.trust.web.dto.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final BranchRepository branchRepository;
    private final DailyEntryRepository dailyEntryRepository;
    private final ItemRepository itemRepository;
    private final RecommendationRepository recommendationRepository;
    private final HealthScoreService healthScoreService;
    private final ItemService itemService;
    private final DecisionRepository decisionRepository;
    private final GroupOrderParticipantRepository groupOrderParticipantRepository;
    private final DecisionAnalyticsService decisionAnalyticsService;

    public DashboardService(BranchRepository branchRepository, DailyEntryRepository dailyEntryRepository,
                             ItemRepository itemRepository, RecommendationRepository recommendationRepository,
                             HealthScoreService healthScoreService, ItemService itemService,
                             DecisionRepository decisionRepository,
                             GroupOrderParticipantRepository groupOrderParticipantRepository,
                             DecisionAnalyticsService decisionAnalyticsService) {
        this.branchRepository = branchRepository;
        this.dailyEntryRepository = dailyEntryRepository;
        this.itemRepository = itemRepository;
        this.recommendationRepository = recommendationRepository;
        this.healthScoreService = healthScoreService;
        this.itemService = itemService;
        this.decisionRepository = decisionRepository;
        this.groupOrderParticipantRepository = groupOrderParticipantRepository;
        this.decisionAnalyticsService = decisionAnalyticsService;
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

        HealthScoreDto healthScore = branches.size() == 1
                ? healthScoreService.calculate(branches.get(0), from, to)
                : averageHealthScore(branches, from, to);

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

        return new DashboardResponse(salesToday, salesChange, totalProfit, profitChange, marginPercent, marginChange,
                availableLiquidity, liquidityChange, healthScore, salesTrend, topRecs,
                inventoryBreakdown, liquidityBreakdown, attention, dailyPerformanceSummary, performanceImpactSummary);
    }

    /** ملخص الأداء اليومي (رؤية PM: الفرص/المخاطر مباشرة على الشاشة الرئيسية بدل أرقام مجردة) */
    private DailyPerformanceSummaryDto buildDailyPerformanceSummary(Long organizationId, List<Long> branchIds,
                                                                      List<Item> items, HealthScoreDto healthScore) {
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
                healthScore.inventoryScore(), purchaseVolumeNeeded, clearanceVolumeNeeded);
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

    private HealthScoreDto averageHealthScore(List<Branch> branches, LocalDate from, LocalDate to) {
        List<HealthScoreDto> scores = branches.stream()
                .map(b -> healthScoreService.calculate(b, from, to)).toList();
        int n = scores.size();
        double total = scores.stream().mapToDouble(HealthScoreDto::totalScore).average().orElse(0);
        return new HealthScoreDto(
                avg(scores, HealthScoreDto::salesScore), avg(scores, HealthScoreDto::profitScore),
                avg(scores, HealthScoreDto::pricingScore), avg(scores, HealthScoreDto::purchasesScore),
                avg(scores, HealthScoreDto::inventoryScore), avg(scores, HealthScoreDto::liquidityScore),
                total, total >= 81 ? "ممتاز" : total >= 61 ? "جيد" : total >= 41 ? "مقبول" : "ضعيف"
        );
    }

    private double avg(List<HealthScoreDto> scores, java.util.function.ToDoubleFunction<HealthScoreDto> f) {
        return Math.round(scores.stream().mapToDouble(f).average().orElse(0) * 10) / 10.0;
    }
}
