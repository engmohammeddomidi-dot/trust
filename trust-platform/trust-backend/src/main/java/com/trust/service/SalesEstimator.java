package com.trust.service;

import com.trust.domain.Item;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * تقدير متوسط المبيعات اليومية لكل صنف. مشترك بين محرك التوصيات ومحرك قرار الشراء
 * حتى لا يختلف التقدير بين الاثنين لنفس الصنف.
 *
 * التقدير القديم (المحفوظ أدناه كتراجع آمن) كان يفترض أن الصنف يبيع نسبة ثابتة من
 * كميته يوميًا. ذلك دائري: أيام التغطية تصبح ثابتة مهما بلغ المخزون، ومجموع المبيعات
 * المقدَّرة لكل الأصناف لا علاقة له بمبيعات الفرع الحقيقية - عمليًا كان يقول إن 2,800
 * دجاجة تنفد خلال 1.3 يوم في محل مبيعاته 3,500 شيكل يوميًا.
 *
 * البديل يُثبِّت المجموع على تكلفة البضاعة المباعة الفعلية، ويوزّعها على الأصناف بحسب
 * فئة حركتها وحدها.
 *
 * التوزيع بحسب قيمة المخزون كان سيبدو أدقّ لكنه يعيد الدائرية نفسها: نصيب الصنف يصير
 * متناسبًا مع مخزونه، فتُختصر الكمية من طرفَي القسمة وتخرج أيام التغطية متساوية لكل
 * أصناف الفئة مهما كان المكدَّس منها والشحيح. التوزيع بحسب الحركة فقط يُبقي المخزون
 * متغيّرًا حقيقيًا: المكدَّس تغطيته أطول، والشحيح يستحق قرار إعادة طلب.
 *
 * يبقى تقريبًا - لا يوجد سجل مبيعات لكل صنف بعد - لكنه مقيَّد برقم حقيقي، ويستجيب
 * للإشارة الوحيدة المستقلة المتاحة عن الطلب: حالة حركة الصنف.
 */
final class SalesEstimator {

    private SalesEstimator() {
    }

    /** ترجيح سرعة الحركة عند توزيع المبيعات الفعلية على الأصناف */
    private static double movementWeight(Item.MovementStatus status) {
        return switch (status) {
            case FAST -> 1.0;
            case MEDIUM -> 0.6;
            case SLOW -> 0.25;
            case STAGNANT -> 0.0;
        };
    }

    /**
     * @param branchDailyCogs تكلفة البضاعة المباعة يوميًا للفرع - صفر يعني لا بيانات
     *                        مبيعات، فيُستخدم التقدير القديم بدل ترك المحركات بلا رقم
     * @return وحدات مباعة يوميًا لكل صنف، مفهرسة بمعرّف الصنف
     */
    static Map<Long, Double> forBranch(List<Item> items, double branchDailyCogs) {
        Map<Long, Double> result = new LinkedHashMap<>();
        if (items.isEmpty()) return result;

        double totalWeight = items.stream()
                .filter(i -> i.getCostPrice() > 0)
                .mapToDouble(i -> movementWeight(i.getMovementStatus()))
                .sum();

        if (branchDailyCogs <= 0 || totalWeight <= 0) {
            for (Item i : items) {
                result.put(i.getId(), legacyEstimate(i));
            }
            return result;
        }

        for (Item i : items) {
            if (i.getCostPrice() <= 0) {
                result.put(i.getId(), 0.0); // بلا تكلفة لا يمكن تحويل القيمة إلى وحدات
                continue;
            }
            double share = movementWeight(i.getMovementStatus()) / totalWeight;
            result.put(i.getId(), (branchDailyCogs * share) / i.getCostPrice());
        }
        return result;
    }

    /**
     * التقدير القديم - يبقى للحالات التي لا تتوفر فيها مبيعات فعلية بعد (مؤسسة جديدة
     * لم تُدخل بياناتها اليومية).
     */
    static double legacyEstimate(Item item) {
        return switch (item.getMovementStatus()) {
            case FAST -> Math.max(1, item.getQuantity() * 0.15);
            case MEDIUM -> Math.max(0.5, item.getQuantity() * 0.07);
            case SLOW -> Math.max(0.2, item.getQuantity() * 0.03);
            case STAGNANT -> 0.0;
        };
    }

    /** توافقًا مع النداءات التي لا تملك سياق الفرع بعد */
    static double estimateDailySales(Item item) {
        return legacyEstimate(item);
    }
}
