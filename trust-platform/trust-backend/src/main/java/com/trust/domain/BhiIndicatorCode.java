package com.trust.domain;

/**
 * المؤشرات الثلاثة عشر لمؤشر صحة الأعمال، كما وردت في ورقة المعايير بالنموذج المرجعي.
 *
 * حقل missingDataHint يشرح للمستخدم ما الذي ينقص حتى يصبح المؤشر قابلًا للحساب - هذا
 * جوهري للصدق: النظام لا يخترع درجة لمؤشر لا يملك بياناته، بل يقول صراحةً ما ينقصه.
 */
public enum BhiIndicatorCode {

    // ---------- الربحية ----------
    NET_PROFIT_MARGIN("هامش صافي الربح", BhiAxis.PROFITABILITY,
            BhiDirection.HIGHER_BETTER, BhiUnit.PERCENT,
            "يتطلب إدخال المصاريف التشغيلية الشهرية",
            0.00, 0.02, 0.05),

    GROSS_PROFIT_MARGIN("هامش الربح الإجمالي", BhiAxis.PROFITABILITY,
            BhiDirection.HIGHER_BETTER, BhiUnit.PERCENT,
            "يتطلب إدخالات يومية تتضمن المبيعات وتكلفة البضاعة",
            0.15, 0.20, 0.28),

    OPERATING_EXPENSE_RATIO("نسبة المصاريف التشغيلية للمبيعات", BhiAxis.PROFITABILITY,
            BhiDirection.LOWER_BETTER, BhiUnit.PERCENT,
            "يتطلب إدخال المصاريف التشغيلية الشهرية",
            0.25, 0.18, 0.12),

    // ---------- السيولة ----------
    CURRENT_RATIO("نسبة التداول", BhiAxis.LIQUIDITY,
            BhiDirection.HIGHER_BETTER, BhiUnit.RATIO,
            "يتطلب إدخال الذمم الدائنة في الإدخال اليومي",
            0.80, 1.20, 1.80),

    CASH_RATIO("نسبة السيولة النقدية", BhiAxis.LIQUIDITY,
            BhiDirection.HIGHER_BETTER, BhiUnit.RATIO,
            "يتطلب إدخال السيولة المتاحة والذمم الدائنة",
            0.10, 0.25, 0.50),

    CASH_CONVERSION_CYCLE("دورة التحويل النقدي", BhiAxis.LIQUIDITY,
            BhiDirection.LOWER_BETTER, BhiUnit.DAYS,
            "يتطلب بيانات المخزون والذمم المدينة والدائنة",
            60, 35, 15),

    // ---------- الكفاءة التشغيلية ----------
    SALES_GROWTH("معدل نمو المبيعات", BhiAxis.OPERATIONAL_EFFICIENCY,
            BhiDirection.HIGHER_BETTER, BhiUnit.PERCENT,
            "يتطلب إدخالات يومية تغطي الفترة الحالية والسابقة",
            -0.05, 0.03, 0.10),

    // ---------- إدارة المخزون ----------
    INVENTORY_TURNOVER("معدل دوران المخزون", BhiAxis.INVENTORY_MANAGEMENT,
            BhiDirection.HIGHER_BETTER, BhiUnit.TIMES_PER_YEAR,
            "يتطلب أصنافًا مسجلة وتكلفة بضاعة مباعة",
            4, 8, 12),

    WASTE_RATIO("نسبة الهدر", BhiAxis.INVENTORY_MANAGEMENT,
            BhiDirection.LOWER_BETTER, BhiUnit.PERCENT,
            "يتطلب سجل توالف - غير متوفر في النظام بعد",
            0.05, 0.02, 0.005),

    STOCK_ACCURACY("دقة الجرد", BhiAxis.INVENTORY_MANAGEMENT,
            BhiDirection.HIGHER_BETTER, BhiUnit.PERCENT,
            "يتطلب عملية جرد فعلي - غير متوفرة في النظام بعد",
            0.85, 0.93, 0.98),

    // ---------- الذمم والمديونية ----------
    DAYS_SALES_OUTSTANDING("أيام تحصيل الذمم", BhiAxis.RECEIVABLES_DEBT,
            BhiDirection.LOWER_BETTER, BhiUnit.DAYS,
            "يتطلب إدخال الذمم المدينة في الإدخال اليومي",
            60, 30, 15),

    PAYMENT_EFFICIENCY("كفاءة السداد", BhiAxis.RECEIVABLES_DEBT,
            BhiDirection.HIGHER_BETTER, BhiUnit.PERCENT,
            "يتطلب تواريخ استحقاق وسداد على المشتريات - غير متوفرة بعد",
            0.60, 0.80, 0.95),

    DEBT_TO_EQUITY("نسبة الدين إلى حقوق الملكية", BhiAxis.RECEIVABLES_DEBT,
            BhiDirection.LOWER_BETTER, BhiUnit.RATIO,
            "يتطلب إدخال حقوق الملكية في إعدادات المؤسسة",
            1.50, 0.70, 0.30);

    private final String labelAr;
    private final BhiAxis axis;
    private final BhiDirection direction;
    private final BhiUnit unit;
    private final String missingDataHint;
    private final double defaultWeak;
    private final double defaultMedium;
    private final double defaultExcellent;

    BhiIndicatorCode(String labelAr, BhiAxis axis, BhiDirection direction,
                     BhiUnit unit, String missingDataHint,
                     double defaultWeak, double defaultMedium, double defaultExcellent) {
        this.labelAr = labelAr;
        this.axis = axis;
        this.direction = direction;
        this.unit = unit;
        this.missingDataHint = missingDataHint;
        this.defaultWeak = defaultWeak;
        this.defaultMedium = defaultMedium;
        this.defaultExcellent = defaultExcellent;
    }

    public String getLabelAr() {
        return labelAr;
    }

    public BhiAxis getAxis() {
        return axis;
    }

    public BhiDirection getDirection() {
        return direction;
    }

    public BhiUnit getUnit() {
        return unit;
    }

    public String getMissingDataHint() {
        return missingDataHint;
    }

    /**
     * الحدود الافتراضية من ورقة المعايير المرجعية. تُستخدم ما لم يضع المشرف تجاوزًا
     * صريحًا لفئة نشاط معينة في جدول bhi_thresholds - فالجدول متفرّق (overrides only)
     * ولا يحتاج بذرًا لثلاثة عشر صفًا مضروبةً في كل فئة.
     */
    public double getDefaultWeak() {
        return defaultWeak;
    }

    public double getDefaultMedium() {
        return defaultMedium;
    }

    public double getDefaultExcellent() {
        return defaultExcellent;
    }
}
