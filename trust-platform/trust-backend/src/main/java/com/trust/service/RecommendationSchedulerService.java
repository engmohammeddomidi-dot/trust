package com.trust.service;

import com.trust.domain.Branch;
import com.trust.domain.Recommendation;
import com.trust.repository.BranchRepository;
import com.trust.repository.RecommendationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * يشغّل محرك التوصيات تلقائيًا مرة يوميًا لكل الفروع - يغني عن الاستدعاء اليدوي
 * لـ /api/recommendations/regenerate (القسم 9.1 من خطة MVP).
 */
@Service
public class RecommendationSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationSchedulerService.class);

    private final BranchRepository branchRepository;
    private final RecommendationEngineService engineService;
    private final RecommendationRepository recommendationRepository;

    public RecommendationSchedulerService(BranchRepository branchRepository,
                                           RecommendationEngineService engineService,
                                           RecommendationRepository recommendationRepository) {
        this.branchRepository = branchRepository;
        this.engineService = engineService;
        this.recommendationRepository = recommendationRepository;
    }

    /** يوميًا الساعة 6:00 صباحًا بتوقيت الخادم */
    @Scheduled(cron = "0 0 6 * * *")
    public void regenerateAllBranches() {
        List<Branch> branches = branchRepository.findAll();
        int total = 0;
        for (Branch branch : branches) {
            List<Recommendation> generated = engineService.generateForBranch(branch);
            recommendationRepository.saveAll(generated);
            total += generated.size();
        }
        log.info("Scheduled recommendation regeneration completed for {} branch(es), {} recommendation(s) upserted", branches.size(), total);
    }
}
