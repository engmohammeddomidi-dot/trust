package com.trust.domain;

/**
 * بنود المصاريف التشغيلية الشهرية - مأخوذة حرفيًا من جدول المصاريف في نموذج مدير
 * المنتج (المدير، موظف رف، الكهرباء، الاجرة، خدمات تقنية، لوازم نثرية).
 */
public enum ExpenseCategory {

    MANAGER_SALARY("راتب المدير"),
    STAFF_SALARY("رواتب الموظفين"),
    ELECTRICITY("الكهرباء"),
    RENT("الإيجار"),
    TECH_SERVICES("خدمات تقنية"),
    SUNDRIES("لوازم نثرية"),
    OTHER("أخرى");

    private final String labelAr;

    ExpenseCategory(String labelAr) {
        this.labelAr = labelAr;
    }

    public String getLabelAr() {
        return labelAr;
    }
}
