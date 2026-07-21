package com.trust.service;

import com.trust.domain.Branch;
import com.trust.domain.Decision;
import com.trust.domain.Recommendation;
import com.trust.repository.BranchRepository;
import com.trust.repository.DecisionRepository;
import com.trust.repository.RecommendationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * يشغّل محرك التوصيات ومحرك قرار الشراء تلقائيًا مرة يوميًا لكل الفروع - يغني عن
 * الاستدعاء اليدوي لـ /api/recommendations/regenerate و /api/decisions/regenerate
 * (القسم 9.1 من خطة MVP).
 */
@Service
public class RecommendationSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationSchedulerService.class);

    private final BranchRepository branchRepository;
    private final RecommendationEngineService engineService;
    private final RecommendationRepository recommendationRepository;
    private final PurchaseDecisionEngineService decisionEngineService;
    private final DecisionRepository decisionRepository;
    private final HealthScoreService healthScoreService;

    public RecommendationSchedulerService(BranchRepository branchRepository,
                                           RecommendationEngineService engineService,
                                           RecommendationRepository recommendationRepository,
                                           PurchaseDecisionEngineService decisionEngineService,
                                           DecisionRepository decisionRepository,
                                           HealthScoreService healthScoreService) {
        this.branchRepository = branchRepository;
        this.engineService = engineService;
        this.recommendationRepository = recommendationRepository;
        this.decisionEngineService = decisionEngineService;
        this.decisionRepository = decisionRepository;
        this.healthScoreService = healthScoreService;
    }

    /** يوميًا الساعة 6:00 صباحًا بتوقيت الخادم */
    @Scheduled(cron = "0 0 6 * * *")
    public void regenerateAllBranches() {
        List<Branch> branches = branchRepository.findAll();
        int totalRecommendations = 0;
        int totalDecisions = 0;
        for (Branch branch : branches) {
            List<Recommendation> generatedRecommendations = engineService.generateForBranch(branch);
            recommendationRepository.saveAll(generatedRecommendations);
            totalRecommendations += generatedRecommendations.size();

            List<Decision> generatedDecisions = decisionEngineService.generateForBranch(branch);
            decisionRepository.saveAll(generatedDecisions);
            totalDecisions += generatedDecisions.size();

            healthScoreService.snapshotToday(branch);
        }
        log.info("Scheduled regeneration completed for {} branch(es): {} recommendation(s), {} decision(s) upserted",
                branches.size(), totalRecommendations, totalDecisions);
    }
}
