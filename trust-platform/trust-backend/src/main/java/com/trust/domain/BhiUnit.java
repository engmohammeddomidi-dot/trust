package com.trust.domain;

/** وحدة قياس المؤشر - تحدد كيف تُعرض القيمة الخام في نص الشرح */
public enum BhiUnit {
    /** كسر عشري يُعرض كنسبة مئوية، مثل 0.225 => 22.5% */
    PERCENT("%"),
    /** نسبة مجردة، مثل نسبة التداول 1.2 */
    RATIO(""),
    /** عدد أيام */
    DAYS("يوم"),
    /** عدد مرات في السنة */
    TIMES_PER_YEAR("مرة/سنة");

    private final String suffixAr;

    BhiUnit(String suffixAr) {
        this.suffixAr = suffixAr;
    }

    public String getSuffixAr() {
        return suffixAr;
    }
}
