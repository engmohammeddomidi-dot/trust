package com.trust.service;

import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * يولّد بدائل حول الكمية الموصى بها - "لا توجد توصية دون بدائل" في رؤية المنتج:
 * التوصية الواحدة تُقرأ كأمر، والثلاثة تُقرأ كاستشارة تُبقي القرار بيد صاحب المحل.
 *
 * ليست محركًا جديدًا: الرياضيات نفسها (تغطية مقابل مهلة توريد ومخزون أمان) موجودة
 * أصلًا، وهذا يولّد شبكة صغيرة حولها.
 *
 * الشرط الحاسم: كل بديل يمر بسقف السيولة نفسه الذي تمر به الكمية الموصى بها. عرض
 * خيار تمنعه سياسة المؤسسة أسوأ من عدم عرض بدائل إطلاقًا.
 */
@Component
public class DecisionAlternativeBuilder {

    private static final DecimalFormat QTY = new DecimalFormat("#,##0");
    private static final double CONSERVATIVE_FACTOR = 0.6;
    private static final double EXTENDED_FACTOR = 1.3;

    public record Alternative(
            String key,
            String label,
            double quantity,
            double orderValue,
            double coverageDays,
            boolean recommended,
            boolean liquidityLimited,
            String tradeOff
    ) {}

    /**
     * @param availableLiquidity صفر يعني لا بيانات سيولة - لا يُطبَّق سقف حينها
     */
    public List<Alternative> build(double recommendedQuantity, double unitCost,
                                   double availableLiquidity, double liquidityRatio,
                                   double dailySales, int reorderThresholdDays) {

        double cap = availableLiquidity > 0 && unitCost > 0
                ? Math.floor((availableLiquidity * liquidityRatio) / unitCost)
                : Double.MAX_VALUE;

        // LinkedHashMap يحفظ ترتيب العرض (متحفّظ → موصى به → موسّع) ويطوي أي خيارين
        // انتهيا لنفس الكمية بعد القص - ثلاثة خيارات متطابقة تُوهم باختيار غير موجود
        Map<Double, Alternative> byQuantity = new LinkedHashMap<>();

        addOption(byQuantity, "CONSERVATIVE", "خيار متحفّظ",
                Math.floor(recommendedQuantity * CONSERVATIVE_FACTOR), false,
                "رأس مال مجمَّد أقل، لكن هامش الأمان أضيق أمام أي ارتفاع مفاجئ في الطلب",
                cap, unitCost, dailySales);

        addOption(byQuantity, "RECOMMENDED", "الكمية الموصى بها",
                Math.floor(recommendedQuantity), true,
                "أفضل توازن بين تغطية الطلب المتوقَّع وعدم تجميد سيولة زائدة",
                cap, unitCost, dailySales);

        addOption(byQuantity, "EXTENDED", "خيار موسّع",
                Math.floor(recommendedQuantity * EXTENDED_FACTOR), false,
                "تغطية أطول وطلبيات أقل تكرارًا، مقابل سيولة مجمَّدة أكبر",
                cap, unitCost, dailySales);

        List<Alternative> options = new ArrayList<>(byQuantity.values());

        // إن طوى القص كل الخيارات إلى واحد، يجب أن يبقى ذلك الواحد هو الموصى به
        if (options.size() == 1 && !options.get(0).recommended()) {
            Alternative only = options.get(0);
            options.set(0, new Alternative(only.key(), only.label(), only.quantity(), only.orderValue(),
                    only.coverageDays(), true, only.liquidityLimited(), only.tradeOff()));
        }
        return options;
    }

    private void addOption(Map<Double, Alternative> target, String key, String label,
                           double desiredQuantity, boolean recommended, String tradeOff,
                           double cap, double unitCost, double dailySales) {
        double quantity = Math.max(1, Math.min(desiredQuantity, cap));
        boolean limited = desiredQuantity > cap;

        // خيار سبق تسجيله بنفس الكمية: نُبقي الموصى به إن كان أحدهما موصى به
        Alternative existing = target.get(quantity);
        if (existing != null && !recommended) return;

        String finalTradeOff = limited
                ? tradeOff + " (قُصَّت الكمية لتبقى ضمن سقف السيولة: %s وحدة)".formatted(QTY.format(cap))
                : tradeOff;

        target.put(quantity, new Alternative(key, label, quantity, quantity * unitCost,
                dailySales > 0 ? quantity / dailySales : 0, recommended, limited, finalTradeOff));
    }
}
