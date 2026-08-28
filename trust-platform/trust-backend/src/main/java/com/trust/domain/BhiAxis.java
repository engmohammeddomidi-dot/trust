package com.trust.domain;

/**
 * محاور مؤشر صحة الأعمال الخمسة، بأوزانها الافتراضية من النموذج المرجعي المعتمد
 * من مدير المنتج. الأوزان هنا افتراضية فقط - القيم الفعلية تُقرأ من جدول
 * bhi_axis_weights لكل فئة نشاط، ويمكن للمشرف تعديلها دون نشر جديد.
 */
public enum BhiAxis {

    PROFITABILITY("الربحية", 0.30),
    LIQUIDITY("السيولة", 0.20),
    OPERATIONAL_EFFICIENCY("الكفاءة التشغيلية", 0.20),
    INVENTORY_MANAGEMENT("إدارة المخزون", 0.15),
    RECEIVABLES_DEBT("الذمم والمديونية", 0.15);

    private final String labelAr;
    private final double defaultWeight;

    BhiAxis(String labelAr, double defaultWeight) {
        this.labelAr = labelAr;
        this.defaultWeight = defaultWeight;
    }

    public String getLabelAr() {
        return labelAr;
    }

    public double getDefaultWeight() {
        return defaultWeight;
    }
}
