package com.trust.web;

import com.trust.domain.BhiAxis;
import com.trust.domain.BhiIndicatorCode;
import com.trust.domain.Category;
import com.trust.service.BhiConfigService;
import com.trust.web.dto.BhiConfigDto;
import com.trust.web.dto.BhiThresholdUpdateRequest;
import com.trust.web.dto.BhiWeightUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * معايرة نموذج BHI لكل فئة نشاط - حدود المؤشرات الثلاثة عشر وأوزان المحاور الخمسة.
 *
 * وجود هذه الشاشة هو الفرق بين نموذج مكتوب في الشيفرة ونموذج مملوك لفريق المنتج:
 * ما يُعدّ "دورانًا ممتازًا" لصيدلية ليس ما يُعدّ كذلك لسوبرماركت.
 */
@RestController
@RequestMapping("/api/admin/bhi-config")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminBhiConfigController {

    private final BhiConfigService configService;

    public AdminBhiConfigController(BhiConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    public BhiConfigDto get(@RequestParam Category category) {
        List<BhiConfigDto.IndicatorConfig> indicators = configService.thresholdsFor(category).stream()
                .map(t -> new BhiConfigDto.IndicatorConfig(
                        t.code().name(), t.code().getLabelAr(), t.code().getAxis().name(),
                        t.code().getDirection().name(), t.code().getUnit().name(),
                        t.weak(), t.medium(), t.excellent(), t.overridden()))
                .toList();

        List<BhiConfigDto.AxisConfig> axes = configService.weightsFor(category).stream()
                .map(w -> new BhiConfigDto.AxisConfig(
                        w.axis().name(), w.axis().getLabelAr(), w.weight(), w.overridden()))
                .toList();

        double weightSum = axes.stream().mapToDouble(BhiConfigDto.AxisConfig::weight).sum();
        return new BhiConfigDto(category.name(), indicators, axes, weightSum);
    }

    @PutMapping("/thresholds")
    public BhiConfigDto updateThreshold(@Valid @RequestBody BhiThresholdUpdateRequest request) {
        configService.upsertThreshold(request.category(), BhiIndicatorCode.valueOf(request.code()),
                request.weak(), request.medium(), request.excellent());
        return get(request.category());
    }

    @PutMapping("/weights")
    public BhiConfigDto updateWeight(@Valid @RequestBody BhiWeightUpdateRequest request) {
        configService.upsertWeight(request.category(), BhiAxis.valueOf(request.axis()), request.weight());
        return get(request.category());
    }

    @DeleteMapping("/thresholds/{code}")
    public BhiConfigDto resetThreshold(@PathVariable String code, @RequestParam Category category) {
        configService.resetThreshold(category, BhiIndicatorCode.valueOf(code));
        return get(category);
    }

    @DeleteMapping("/weights/{axis}")
    public BhiConfigDto resetWeight(@PathVariable String axis, @RequestParam Category category) {
        configService.resetWeight(category, BhiAxis.valueOf(axis));
        return get(category);
    }
}
