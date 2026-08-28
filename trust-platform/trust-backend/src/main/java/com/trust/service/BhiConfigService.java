package com.trust.service;

import com.trust.domain.*;
import com.trust.repository.BhiAxisWeightRepository;
import com.trust.repository.BhiThresholdRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * قراءة وتعديل حدود وأوزان BHI لكل فئة نشاط.
 *
 * الجدولان متفرّقان (تجاوزات فقط)، فالقراءة تدمج القيم الافتراضية من enum مع أي تجاوز
 * مسجَّل. هذا ما يجعل نموذج مدير المنتج قابلًا للمعايرة دون نشر جديد، ويسمح بأن تختلف
 * الصيدلية عن السوبرماركت في ما يُعدّ "دورانًا ممتازًا".
 */
@Service
public class BhiConfigService {

    private final BhiThresholdRepository thresholdRepository;
    private final BhiAxisWeightRepository axisWeightRepository;

    public BhiConfigService(BhiThresholdRepository thresholdRepository,
                            BhiAxisWeightRepository axisWeightRepository) {
        this.thresholdRepository = thresholdRepository;
        this.axisWeightRepository = axisWeightRepository;
    }

    public record EffectiveThreshold(BhiIndicatorCode code, double weak, double medium,
                                     double excellent, boolean overridden) {}

    public record EffectiveWeight(BhiAxis axis, double weight, boolean overridden) {}

    public List<EffectiveThreshold> thresholdsFor(Category category) {
        Map<BhiIndicatorCode, BhiThreshold> overrides = new EnumMap<>(BhiIndicatorCode.class);
        thresholdRepository.findByCategory(category).forEach(t -> overrides.put(t.getCode(), t));

        List<EffectiveThreshold> result = new ArrayList<>();
        for (BhiIndicatorCode code : BhiIndicatorCode.values()) {
            BhiThreshold o = overrides.get(code);
            result.add(o != null
                    ? new EffectiveThreshold(code, o.getWeakThreshold(), o.getMediumThreshold(),
                        o.getExcellentThreshold(), true)
                    : new EffectiveThreshold(code, code.getDefaultWeak(), code.getDefaultMedium(),
                        code.getDefaultExcellent(), false));
        }
        return result;
    }

    public List<EffectiveWeight> weightsFor(Category category) {
        Map<BhiAxis, BhiAxisWeight> overrides = new EnumMap<>(BhiAxis.class);
        axisWeightRepository.findByCategory(category).forEach(w -> overrides.put(w.getAxis(), w));

        List<EffectiveWeight> result = new ArrayList<>();
        for (BhiAxis axis : BhiAxis.values()) {
            BhiAxisWeight o = overrides.get(axis);
            result.add(o != null
                    ? new EffectiveWeight(axis, o.getWeight(), true)
                    : new EffectiveWeight(axis, axis.getDefaultWeight(), false));
        }
        return result;
    }

    @Transactional
    public void upsertThreshold(Category category, BhiIndicatorCode code,
                                double weak, double medium, double excellent) {
        BhiThreshold row = thresholdRepository.findByCategory(category).stream()
                .filter(t -> t.getCode() == code)
                .findFirst()
                .orElseGet(() -> {
                    BhiThreshold t = new BhiThreshold();
                    t.setCategory(category);
                    t.setCode(code);
                    return t;
                });
        row.setWeakThreshold(weak);
        row.setMediumThreshold(medium);
        row.setExcellentThreshold(excellent);
        thresholdRepository.save(row);
    }

    @Transactional
    public void upsertWeight(Category category, BhiAxis axis, double weight) {
        BhiAxisWeight row = axisWeightRepository.findByCategory(category).stream()
                .filter(w -> w.getAxis() == axis)
                .findFirst()
                .orElseGet(() -> {
                    BhiAxisWeight w = new BhiAxisWeight();
                    w.setCategory(category);
                    w.setAxis(axis);
                    return w;
                });
        row.setWeight(weight);
        axisWeightRepository.save(row);
    }

    /** حذف التجاوز يعيد المؤشر إلى قيمة النموذج المرجعي الافتراضية */
    @Transactional
    public void resetThreshold(Category category, BhiIndicatorCode code) {
        thresholdRepository.findByCategory(category).stream()
                .filter(t -> t.getCode() == code)
                .forEach(thresholdRepository::delete);
    }

    @Transactional
    public void resetWeight(Category category, BhiAxis axis) {
        axisWeightRepository.findByCategory(category).stream()
                .filter(w -> w.getAxis() == axis)
                .forEach(axisWeightRepository::delete);
    }
}
