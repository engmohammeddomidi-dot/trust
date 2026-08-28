package com.trust.web.dto;

import com.trust.domain.BhiAxis;
import com.trust.domain.BhiIndicatorCode;

import java.util.List;

/**
 * نتيجة مؤشر صحة الأعمال (BHI) كاملةً بتفصيلها - وليس رقمًا مجردًا.
 *
 * totalScore قد يكون فارغًا عمدًا حين لا تتوفر بيانات كافية لأي مؤشر؛ عرض صفر أو
 * خمسين في تلك الحالة سيكون رقمًا مختلقًا، وهو ما ترفضه رؤية "الذكاء القابل للتفسير".
 */
public record BhiResultDto(
        Double totalScore,
        String label,
        int availableIndicatorCount,
        int totalIndicatorCount,
        List<AxisScore> axes
) {

    /**
     * درجة محور واحد. score فارغة تعني أن المحور بلا أي مؤشر متاح - يظهر في القائمة
     * ولا يُحذف، حتى يعرف المستخدم أن هناك جانبًا لم يُقيَّم، لكنه يُستبعد من الوزن.
     */
    public record AxisScore(
            BhiAxis axis,
            String labelAr,
            Double score,
            double weight,
            List<IndicatorScore> indicators
    ) {}

    /**
     * درجة مؤشر واحد مع شرحه المقروء. حين يكون available=false تبقى القيمة والدرجة
     * فارغتين، ويحمل explanation سبب عدم التوفر - فالمستخدم يرى ما ينقصه بدل فراغ صامت.
     */
    public record IndicatorScore(
            BhiIndicatorCode code,
            String labelAr,
            boolean available,
            Double rawValue,
            Double score,
            String band,
            String bandLabelAr,
            String explanation
    ) {}
}
