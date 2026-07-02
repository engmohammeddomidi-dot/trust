package com.trust.web;

import com.trust.domain.Category;
import com.trust.domain.CategoryBenchmark;
import com.trust.repository.CategoryBenchmarkRepository;
import com.trust.web.dto.CategoryBenchmarkDto;
import com.trust.web.dto.UpdateCategoryBenchmarkRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** إدارة معايير المرجعية (Benchmarks) لكل تصنيف نشاط - القسم 8.4 من خطة MVP */
@RestController
@RequestMapping("/api/admin/benchmarks")
public class AdminBenchmarkController {

    private final CategoryBenchmarkRepository benchmarkRepository;

    public AdminBenchmarkController(CategoryBenchmarkRepository benchmarkRepository) {
        this.benchmarkRepository = benchmarkRepository;
    }

    @GetMapping
    public List<CategoryBenchmarkDto> list() {
        return benchmarkRepository.findAll().stream().map(AdminBenchmarkController::toDto).toList();
    }

    @PutMapping("/{category}")
    public CategoryBenchmarkDto update(@PathVariable Category category, @Valid @RequestBody UpdateCategoryBenchmarkRequest request) {
        CategoryBenchmark bm = benchmarkRepository.findById(category).orElseGet(() -> {
            CategoryBenchmark fresh = new CategoryBenchmark();
            fresh.setCategory(category);
            return fresh;
        });
        bm.setTargetMarginPercent(request.targetMarginPercent());
        bm.setLiquidityRatioMin(request.liquidityRatioMin());
        bm.setLiquidityRatioMax(request.liquidityRatioMax());
        bm.setInventoryCoverageMinMonths(request.inventoryCoverageMinMonths());
        bm.setInventoryCoverageMaxMonths(request.inventoryCoverageMaxMonths());
        bm.setStagnationDaysThreshold(request.stagnationDaysThreshold());
        bm.setSlowMovingDaysThreshold(request.slowMovingDaysThreshold());
        bm.setMediumMovingDaysThreshold(request.mediumMovingDaysThreshold());
        bm.setWeightSales(request.weightSales());
        bm.setWeightProfit(request.weightProfit());
        bm.setWeightPricing(request.weightPricing());
        bm.setWeightPurchases(request.weightPurchases());
        bm.setWeightInventory(request.weightInventory());
        bm.setWeightLiquidity(request.weightLiquidity());
        return toDto(benchmarkRepository.save(bm));
    }

    private static CategoryBenchmarkDto toDto(CategoryBenchmark bm) {
        return new CategoryBenchmarkDto(
                bm.getCategory().name(), bm.getTargetMarginPercent(), bm.getLiquidityRatioMin(), bm.getLiquidityRatioMax(),
                bm.getInventoryCoverageMinMonths(), bm.getInventoryCoverageMaxMonths(), bm.getStagnationDaysThreshold(),
                bm.getSlowMovingDaysThreshold(), bm.getMediumMovingDaysThreshold(), bm.getWeightSales(), bm.getWeightProfit(),
                bm.getWeightPricing(), bm.getWeightPurchases(), bm.getWeightInventory(), bm.getWeightLiquidity()
        );
    }
}
