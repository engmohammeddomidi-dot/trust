package com.trust.web.dto;

import java.util.List;

/**
 * إعدادات نموذج BHI لفئة نشاط واحدة. overridden يميّز القيمة التي عدّلها المشرف عن
 * القيمة الافتراضية من النموذج المرجعي، حتى يعرف من ينظر إلى الشاشة ما الذي تغيّر.
 */
public record BhiConfigDto(
        String category,
        List<IndicatorConfig> indicators,
        List<AxisConfig> axes,
        /** مجموع أوزان المحاور - يجب أن يساوي 1 تقريبًا، والواجهة تنبّه إن لم يكن كذلك */
        double axisWeightSum
) {
    public record IndicatorConfig(
            String code,
            String labelAr,
            String axis,
            String direction,
            String unit,
            double weak,
            double medium,
            double excellent,
            boolean overridden
    ) {}

    public record AxisConfig(String axis, String labelAr, double weight, boolean overridden) {}
}
