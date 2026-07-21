package com.trust.service;

import com.trust.domain.Decision;
import com.trust.domain.Purchase;
import com.trust.repository.DecisionRepository;
import com.trust.repository.PurchaseRepository;
import com.trust.web.dto.DecisionQualityScoreDto;
import com.trust.web.dto.PerformanceImpactSummaryDto;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * حسابات مشتركة على القرارات - مستخدمة من DecisionController (لفرع واحد) ومن
 * DashboardService (لعدة فروع/المؤسسة كاملة) حتى لا يختلف حساب نفس المؤشر بين الشاشتين.
 */
@Service
public class DecisionAnalyticsService {

    private final DecisionRepository decisionRepository;
    private final PurchaseRepository purchaseRepository;

    public DecisionAnalyticsService(DecisionRepository decisionRepository, PurchaseRepository purchaseRepository) {
        this.decisionRepository = decisionRepository;
        this.purchaseRepository = purchaseRepository;
    }

    public DecisionQualityScoreDto qualityScore(List<Long> branchIds) {
        List<Purchase> issuedFromDecisions = purchaseRepository.findByBranchIdInAndDecisionIsNotNull(branchIds);
        long received = issuedFromDecisions.stream().filter(p -> p.getStatus() == Purchase.Status.RECEIVED).count();
        long withDiscrepancy = issuedFromDecisions.stream()
                .filter(p -> p.getStatus() == Purchase.Status.RECEIVED && p.isHasDiscrepancy()).count();
        Double qualityScore = received > 0 ? Math.round((1 - (double) withDiscrepancy / received) * 1000) / 10.0 : null;
        return new DecisionQualityScoreDto(issuedFromDecisions.size(), (int) received, (int) withDiscrepancy, qualityScore);
    }

    /** مؤشر الأداء والأثر الفعلي + عدد المخاطر/الفرص المُعالَجة - رؤية PM (مركز القيادة) */
    public PerformanceImpactSummaryDto performanceImpactSummary(List<Long> branchIds) {
        Double performanceScore = qualityScore(branchIds).qualityScorePercent();

        List<Decision> decisions = decisionRepository.findByBranchIdIn(branchIds);
        long risksResolved = decisions.stream()
                .filter(d -> d.getCategory() == Decision.Category.RISK && d.getStatus() != Decision.Status.OPEN)
                .count();
        long opportunitiesResolved = decisions.stream()
                .filter(d -> d.getCategory() == Decision.Category.OPPORTUNITY && d.getStatus() != Decision.Status.OPEN)
                .count();
        double completionRate = decisions.isEmpty() ? 0
                : Math.round((double) (risksResolved + opportunitiesResolved) / decisions.size() * 1000) / 10.0;

        return new PerformanceImpactSummaryDto(performanceScore, (int) risksResolved, (int) opportunitiesResolved, completionRate);
    }
}
