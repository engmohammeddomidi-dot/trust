package com.trust.service;

import com.trust.domain.Decision;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * يبني العناصر الثلاثة التي تكمل بطاقة القرار وفق دستور رؤية المنتج:
 *
 *  1. "لو تجاهلت" - الأثر المالي لعدم التنفيذ، لأن التوصية التي تعرض المكسب فقط
 *     تُقرأ كإعلان، بينما التي تعرض الكلفة أيضًا تُقرأ كنصيحة.
 *  2. "القيود" - ما راعاه المحرك فعلًا (سقف السيولة، سياسة المورّد)، فالتوصية
 *     المثالية نظريًا بلا قيود لا يثق بها صاحب المحل.
 *  3. "أسباب الثقة" - لماذا هذه النسبة بالذات، بدل رقم مجرّد لا يمكن مساءلته.
 *
 * كلها مشتقة من قيم يحسبها المحرك أصلًا - لا مصدر بيانات جديد.
 */
@Component
public class DecisionExplanationBuilder {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0");
    private static final DecimalFormat DAYS = new DecimalFormat("0.#");
    private static final String SEPARATOR = " • ";

    /** ماذا يحدث لو لم يُنفَّذ القرار */
    public String ifIgnored(double financialImpact, double stockoutRiskDays) {
        return "لو تجاهلت: يُتوقَّع نفاد الصنف خلال %s يوم، بخسارة ربح تقديرية %s شيكل."
                .formatted(DAYS.format(stockoutRiskDays), MONEY.format(financialImpact));
    }

    /** القيود التي راعاها المحرك عند اقتراح الكمية */
    public List<String> constraints(boolean liquidityCapped, boolean supplierBelowPolicy,
                                    double effectiveLiquidityRatio, String supplierName) {
        List<String> constraints = new ArrayList<>();

        if (liquidityCapped) {
            constraints.add("خُفِّضت الكمية لتبقى ضمن سقف السيولة المسموح به (%s%% من السيولة المتاحة)"
                    .formatted(MONEY.format(effectiveLiquidityRatio * 100)));
        }
        if (supplierBelowPolicy) {
            constraints.add("تقييم المورّد %s أقل من الحد الذي حددته سياسة المؤسسة"
                    .formatted(supplierName != null ? supplierName : "المرتبط بالصنف"));
        }
        if (constraints.isEmpty()) {
            // قولها صراحةً: الفحص جرى ولم يقيّد شيء - الفراغ وحده لا يثبت ذلك
            constraints.add("لا قيود مؤثرة - الكمية المقترحة ضمن السيولة والسياسات المعتمدة");
        }
        return constraints;
    }

    /** لماذا درجة الثقة هي ما هي */
    public List<String> confidenceReasons(boolean hasSupplier, boolean hasLiquidityData,
                                          boolean supplierBelowPolicy) {
        List<String> reasons = new ArrayList<>();

        if (hasSupplier) {
            reasons.add("مورّد مرتبط بالصنف بمهلة توريد معروفة");
        } else {
            reasons.add("لا يوجد مورّد مرتبط بالصنف - استُخدمت مهلة توريد افتراضية");
        }

        if (hasLiquidityData) {
            reasons.add("بيانات السيولة محدَّثة من آخر إدخال يومي");
        } else {
            reasons.add("لا تتوفر بيانات السيولة - لم يُفحص سقف الشراء");
        }

        if (supplierBelowPolicy) {
            reasons.add("تقييم المورّد دون حد السياسة - خُفِّضت الثقة");
        }
        return reasons;
    }

    /** يكتب العناصر الثلاثة على القرار نفسه حتى تبقى معه في السجل */
    public void applyTo(Decision decision, double financialImpact, double stockoutRiskDays,
                        boolean liquidityCapped, boolean supplierBelowPolicy,
                        double effectiveLiquidityRatio, String supplierName,
                        boolean hasSupplier, boolean hasLiquidityData) {
        decision.setIfIgnoredSummary(ifIgnored(financialImpact, stockoutRiskDays));
        decision.setConstraintsSummary(
                String.join(SEPARATOR, constraints(liquidityCapped, supplierBelowPolicy,
                        effectiveLiquidityRatio, supplierName)));
        decision.setConfidenceReasons(
                String.join(SEPARATOR, confidenceReasons(hasSupplier, hasLiquidityData, supplierBelowPolicy)));
    }
}
