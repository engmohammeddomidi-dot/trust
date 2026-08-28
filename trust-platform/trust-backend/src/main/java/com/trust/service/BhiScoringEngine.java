package com.trust.service;

import com.trust.domain.BhiAxis;
import com.trust.domain.BhiDirection;
import com.trust.domain.BhiIndicatorCode;
import com.trust.domain.BhiUnit;
import com.trust.web.dto.BhiResultDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * محرك تسجيل مؤشر صحة الأعمال (BHI) - دالة صرفة بلا أي وصول لقاعدة البيانات، حتى يمكن
 * التحقق منها حرفيًا مقابل النموذج المرجعي المعتمد من مدير المنتج.
 *
 * التحويل خطي متعدد القطع بين ثلاث نقاط ارتكاز مستخرجة بالحساب العكسي من ورقة الحساب:
 *   الحد الضعيف => 40، الحد المتوسط => 70، الحد الممتاز => 100.
 * ما فوق الممتاز يُثبَّت عند 100، وما دون الضعيف يُستكمَل خطيًا بنفس ميل قطعة
 * (ضعيف→متوسط) نزولًا حتى الصفر - هذا الجزء اجتهاد منّا لأن النموذج المرجعي لا يحتوي
 * أي مثال تحت الحد الضعيف.
 *
 * التجميع على مستويين: وزن متساوٍ بين مؤشرات المحور الواحد، ثم أوزان صريحة بين المحاور.
 */
@Component
public class BhiScoringEngine {

    private static final double WEAK_ANCHOR = 40.0;
    private static final double MEDIUM_ANCHOR = 70.0;
    private static final double EXCELLENT_ANCHOR = 100.0;

    /**
     * مدخل مؤشر واحد. value فارغة تعني أن البيانات اللازمة غير متوفرة بعد - وهي حالة
     * متوقعة وليست خطأ (ستة من ثلاثة عشر مؤشرًا تنتظر مصادر بيانات لم تُبنَ بعد).
     */
    public record IndicatorInput(
            BhiIndicatorCode code,
            Double value,
            double weak,
            double medium,
            double excellent
    ) {}

    // ---------------- التحويل ----------------

    public double normalize(BhiDirection direction, double weak, double medium, double excellent, double value) {
        // بقلب الإشارة للمؤشرات "أقل أفضل" يصبح الترتيب دائمًا تصاعديًا: ضعيف < متوسط < ممتاز
        double sign = direction == BhiDirection.LOWER_BETTER ? -1.0 : 1.0;
        double v = sign * value;
        double w = sign * weak;
        double m = sign * medium;
        double e = sign * excellent;

        if (v >= e) return EXCELLENT_ANCHOR;
        if (v >= m) return interpolate(v, m, e, MEDIUM_ANCHOR, EXCELLENT_ANCHOR);
        if (v >= w) return interpolate(v, w, m, WEAK_ANCHOR, MEDIUM_ANCHOR);

        return Math.max(0.0, interpolate(v, w, m, WEAK_ANCHOR, MEDIUM_ANCHOR));
    }

    private double interpolate(double value, double fromValue, double toValue, double fromScore, double toScore) {
        double span = toValue - fromValue;
        if (span == 0) return fromScore; // حدّان متطابقان - لا معنى للتدرّج
        return fromScore + (toScore - fromScore) * ((value - fromValue) / span);
    }

    // ---------------- التجميع ----------------

    public BhiResultDto aggregate(List<IndicatorInput> inputs, Map<BhiAxis, Double> axisWeights) {
        Map<BhiAxis, List<BhiResultDto.IndicatorScore>> byAxis = new EnumMap<>(BhiAxis.class);
        int available = 0;

        for (IndicatorInput input : inputs) {
            BhiResultDto.IndicatorScore scored = scoreIndicator(input);
            if (scored.available()) available++;
            byAxis.computeIfAbsent(input.code().getAxis(), a -> new ArrayList<>()).add(scored);
        }

        List<BhiResultDto.AxisScore> axes = new ArrayList<>();
        double weightedSum = 0;
        double weightTotal = 0;

        // الترتيب حسب تعريف enum حتى يبقى مخرج الـ API ثابتًا مهما كان ترتيب المدخلات
        for (BhiAxis axis : BhiAxis.values()) {
            List<BhiResultDto.IndicatorScore> indicators = byAxis.get(axis);
            if (indicators == null) continue;

            List<Double> scores = indicators.stream()
                    .filter(BhiResultDto.IndicatorScore::available)
                    .map(BhiResultDto.IndicatorScore::score)
                    .toList();
            if (scores.isEmpty()) {
                // يظهر في المخرجات بدرجة فارغة - مرئي للمستخدم، مستبعَد من الحساب
                axes.add(new BhiResultDto.AxisScore(axis, axis.getLabelAr(), null,
                        axisWeights.getOrDefault(axis, axis.getDefaultWeight()), indicators));
                continue;
            }

            // وزن متساوٍ داخل المحور، تمامًا كما في النموذج المرجعي
            double axisScore = scores.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
            double weight = axisWeights.getOrDefault(axis, axis.getDefaultWeight());

            axes.add(new BhiResultDto.AxisScore(axis, axis.getLabelAr(), axisScore, weight, indicators));
            weightedSum += axisScore * weight;
            weightTotal += weight;
        }

        // إعادة توزيع الأوزان على المحاور المتاحة فقط - محور مفقود لا يسحب المؤشر العام لأسفل
        Double total = weightTotal > 0 ? weightedSum / weightTotal : null;

        return new BhiResultDto(
                total,
                total != null ? classify(total) : "غير كافٍ",
                available,
                inputs.size(),
                axes
        );
    }

    /**
     * دمج نتائج عدة فروع في نتيجة واحدة على مستوى المؤسسة. الفروع بلا بيانات كافية
     * تُستبعد من المتوسط بدل احتسابها صفرًا (وهو ما كان سيجرّ الرقم لأسفل زورًا).
     * تفاصيل المؤشرات لا تُدمَج - متوسط قيمة خام لفرعين لا يعني شيئًا تشغيليًا.
     */
    public BhiResultDto average(List<BhiResultDto> results) {
        List<BhiResultDto> scoreable = results.stream()
                .filter(r -> r.totalScore() != null)
                .toList();

        if (scoreable.isEmpty()) {
            return new BhiResultDto(null, "غير كافٍ", 0,
                    results.isEmpty() ? 0 : results.get(0).totalIndicatorCount(), List.of());
        }

        double total = scoreable.stream().mapToDouble(BhiResultDto::totalScore).average().orElseThrow();

        List<BhiResultDto.AxisScore> axes = new ArrayList<>();
        for (BhiAxis axis : BhiAxis.values()) {
            List<BhiResultDto.AxisScore> present = scoreable.stream()
                    .flatMap(r -> r.axes().stream())
                    .filter(a -> a.axis() == axis && a.score() != null)
                    .toList();
            if (present.isEmpty()) continue;

            axes.add(new BhiResultDto.AxisScore(
                    axis,
                    axis.getLabelAr(),
                    present.stream().mapToDouble(BhiResultDto.AxisScore::score).average().orElseThrow(),
                    present.get(0).weight(),
                    List.of()));
        }

        int available = (int) Math.round(scoreable.stream()
                .mapToInt(BhiResultDto::availableIndicatorCount).average().orElse(0));

        return new BhiResultDto(total, classify(total), available,
                scoreable.get(0).totalIndicatorCount(), axes);
    }

    private BhiResultDto.IndicatorScore scoreIndicator(IndicatorInput input) {
        BhiIndicatorCode code = input.code();

        if (input.value() == null) {
            return new BhiResultDto.IndicatorScore(
                    code, code.getLabelAr(), false, null, null,
                    "UNAVAILABLE", "غير متاح", code.getMissingDataHint());
        }

        double value = input.value();
        double score = normalize(code.getDirection(), input.weak(), input.medium(), input.excellent(), value);
        Band band = bandOf(code.getDirection(), input, value);

        return new BhiResultDto.IndicatorScore(
                code, code.getLabelAr(), true, value, score,
                band.name(), band.labelAr, explain(code, input, value, band));
    }

    // ---------------- التصنيف ----------------

    /**
     * حدود التصنيف اجتهاد منّا - النموذج المرجعي يعطي نقطة واحدة فقط (77.21 => "جيدة").
     * اخترنا نقاط القطع من نفس نقاط ارتكاز مقياس الدرجات: 70 (المتوسط) و100 (الممتاز)
     * ومنتصفاهما 85 و55، فتبقى المصطلحات متسقة بين مستوى المؤشر ومستوى النتيجة العامة.
     */
    public String classify(double total) {
        if (total >= 85) return "ممتازة";
        if (total >= 70) return "جيدة";
        if (total >= 55) return "مقبولة";
        return "ضعيفة";
    }

    private enum Band {
        EXCELLENT("ممتاز"), GOOD("جيد"), ACCEPTABLE("مقبول"), WEAK("ضعيف");

        private final String labelAr;

        Band(String labelAr) {
            this.labelAr = labelAr;
        }
    }

    private Band bandOf(BhiDirection direction, IndicatorInput input, double value) {
        double sign = direction == BhiDirection.LOWER_BETTER ? -1.0 : 1.0;
        double v = sign * value;
        if (v >= sign * input.excellent()) return Band.EXCELLENT;
        if (v >= sign * input.medium()) return Band.GOOD;
        if (v >= sign * input.weak()) return Band.ACCEPTABLE;
        return Band.WEAK;
    }

    // ---------------- الشرح المقروء ----------------

    private String explain(BhiIndicatorCode code, IndicatorInput input, double value, Band band) {
        BhiUnit unit = code.getUnit();
        String shown = format(value, unit);

        return switch (band) {
            case EXCELLENT -> "%s %s - عند الحد الممتاز (%s) أو أفضل"
                    .formatted(code.getLabelAr(), shown, format(input.excellent(), unit));
            case GOOD -> "%s %s - بين الحد المتوسط (%s) والحد الممتاز (%s)"
                    .formatted(code.getLabelAr(), shown, format(input.medium(), unit), format(input.excellent(), unit));
            case ACCEPTABLE -> "%s %s - بين الحد الضعيف (%s) والحد المتوسط (%s)"
                    .formatted(code.getLabelAr(), shown, format(input.weak(), unit), format(input.medium(), unit));
            case WEAK -> "%s %s - دون الحد الضعيف (%s)"
                    .formatted(code.getLabelAr(), shown, format(input.weak(), unit));
        };
    }

    private String format(double value, BhiUnit unit) {
        double shown = unit == BhiUnit.PERCENT ? value * 100.0 : value;
        String number = BigDecimal.valueOf(shown)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
        String suffix = unit.getSuffixAr();
        return suffix.isEmpty() ? number : number + " " + suffix;
    }
}
