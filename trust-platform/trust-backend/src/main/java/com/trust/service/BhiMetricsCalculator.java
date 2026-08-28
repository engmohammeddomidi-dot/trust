package com.trust.service;

import com.trust.domain.BhiIndicatorCode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * اشتقاق القيم الخام لمؤشرات BHI من بيانات المنصة. منفصل عن محرك التسجيل عمدًا:
 * هذا يعرف من أين تأتي الأرقام، وذاك يعرف كيف تتحول إلى درجات.
 *
 * كل مؤشر يعيد null حين تنقص بياناته - وهذا ليس خطأ بل الحالة الطبيعية اليوم: أربعة
 * مؤشرات (الهدر، دقة الجرد، كفاءة السداد، الدين إلى حقوق الملكية) تنتظر مصادر بيانات
 * لم تُبنَ في المنصة بعد، ومؤشران يحتاجان المصاريف التشغيلية الشهرية.
 * القسمة على صفر تعطي null أيضًا - لا نعرض لانهاية للمستخدم.
 */
@Component
public class BhiMetricsCalculator {

    private static final double DAYS_PER_YEAR = 365.0;

    public Map<BhiIndicatorCode, Double> compute(RawInputs in) {
        Map<BhiIndicatorCode, Double> values = new EnumMap<>(BhiIndicatorCode.class);

        double sales = in.currentPeriodSales();
        double cogs = in.costOfGoodsSold();
        int days = Math.max(1, in.periodDays());

        // ---------- الربحية ----------
        values.put(BhiIndicatorCode.GROSS_PROFIT_MARGIN, divide(sales - cogs, sales));
        values.put(BhiIndicatorCode.NET_PROFIT_MARGIN, in.operatingExpenses() == null
                ? null
                : divide(sales - cogs - in.operatingExpenses(), sales));
        values.put(BhiIndicatorCode.OPERATING_EXPENSE_RATIO, in.operatingExpenses() == null
                ? null
                : divide(in.operatingExpenses(), sales));

        // ---------- السيولة ----------
        double currentAssets = in.availableLiquidity() + in.inventoryValue() + in.receivables();
        values.put(BhiIndicatorCode.CURRENT_RATIO, divide(currentAssets, in.payables()));
        values.put(BhiIndicatorCode.CASH_RATIO, divide(in.availableLiquidity(), in.payables()));

        // معدل الدوران يُحسب أولًا لأن أيام المخزون تُشتق منه، فلا يتناقض المؤشران أبدًا
        Double turnover = divide(cogs * (DAYS_PER_YEAR / days), in.inventoryValue());
        values.put(BhiIndicatorCode.INVENTORY_TURNOVER, turnover);

        Double dso = divide(in.receivables(), rate(sales, days));
        values.put(BhiIndicatorCode.DAYS_SALES_OUTSTANDING, dso);

        Double dpo = divide(in.payables(), rate(cogs, days));
        Double inventoryDays = divide(DAYS_PER_YEAR, turnover);
        values.put(BhiIndicatorCode.CASH_CONVERSION_CYCLE,
                (inventoryDays == null || dso == null || dpo == null)
                        ? null
                        : inventoryDays + dso - dpo);

        // ---------- الكفاءة التشغيلية ----------
        // المقارنة بين معدّلَي المبيعات اليومية لا بين المجموعين، حتى لا تبدو فترة
        // أقصر انكماشًا في المبيعات وهي ليست كذلك
        int previousDays = Math.max(1, in.previousPeriodDays());
        Double currentRate = rate(sales, days);
        Double previousRate = rate(in.previousPeriodSales(), previousDays);
        values.put(BhiIndicatorCode.SALES_GROWTH,
                (currentRate == null || previousRate == null || previousRate == 0)
                        ? null
                        : (currentRate - previousRate) / previousRate);

        // ---------- في انتظار مصادر بيانات غير موجودة بعد ----------
        values.put(BhiIndicatorCode.WASTE_RATIO, in.wasteRatio());
        values.put(BhiIndicatorCode.STOCK_ACCURACY, in.stockAccuracy());
        values.put(BhiIndicatorCode.PAYMENT_EFFICIENCY, in.paymentEfficiency());
        values.put(BhiIndicatorCode.DEBT_TO_EQUITY, in.debtToEquity());

        return values;
    }

    /** قسمة آمنة - المقام صفر (أو مفقود) يعني "غير متاح" لا لانهاية */
    private Double divide(double numerator, Double denominator) {
        if (denominator == null || denominator == 0) return null;
        return numerator / denominator;
    }

    /**
     * معدل يومي - البسط صفرًا نتيجة حقيقية (لا مبيعات) لا بيانات ناقصة. فقط المقام
     * الصفري يعني "غير متاح".
     */
    private Double rate(double total, double days) {
        if (days == 0) return null;
        return total / days;
    }

    /**
     * مدخلات المؤشرات الخام. الحقول الأربعة الأخيرة nullable لأنها تنتظر ميزات لم تُبنَ:
     * سجل التوالف، الجرد الفعلي، تواريخ سداد المشتريات، وحقوق الملكية.
     */
    public record RawInputs(
            int periodDays,
            int previousPeriodDays,
            double currentPeriodSales,
            double previousPeriodSales,
            double costOfGoodsSold,
            double inventoryValue,
            double availableLiquidity,
            double receivables,
            double payables,
            Double operatingExpenses,
            Double wasteRatio,
            Double stockAccuracy,
            Double paymentEfficiency,
            Double debtToEquity
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private int periodDays = 30;
            private int previousPeriodDays = 30;
            private double currentPeriodSales;
            private double previousPeriodSales;
            private double costOfGoodsSold;
            private double inventoryValue;
            private double availableLiquidity;
            private double receivables;
            private double payables;
            private Double operatingExpenses;
            private Double wasteRatio;
            private Double stockAccuracy;
            private Double paymentEfficiency;
            private Double debtToEquity;

            public Builder periodDays(int v) { this.periodDays = v; return this; }
            public Builder previousPeriodDays(int v) { this.previousPeriodDays = v; return this; }
            public Builder currentPeriodSales(double v) { this.currentPeriodSales = v; return this; }
            public Builder previousPeriodSales(double v) { this.previousPeriodSales = v; return this; }
            public Builder costOfGoodsSold(double v) { this.costOfGoodsSold = v; return this; }
            public Builder inventoryValue(double v) { this.inventoryValue = v; return this; }
            public Builder availableLiquidity(double v) { this.availableLiquidity = v; return this; }
            public Builder receivables(double v) { this.receivables = v; return this; }
            public Builder payables(double v) { this.payables = v; return this; }
            public Builder operatingExpenses(Double v) { this.operatingExpenses = v; return this; }
            public Builder wasteRatio(Double v) { this.wasteRatio = v; return this; }
            public Builder stockAccuracy(Double v) { this.stockAccuracy = v; return this; }
            public Builder paymentEfficiency(Double v) { this.paymentEfficiency = v; return this; }
            public Builder debtToEquity(Double v) { this.debtToEquity = v; return this; }

            public RawInputs build() {
                return new RawInputs(periodDays, previousPeriodDays, currentPeriodSales, previousPeriodSales, costOfGoodsSold,
                        inventoryValue, availableLiquidity, receivables, payables,
                        operatingExpenses, wasteRatio, stockAccuracy, paymentEfficiency, debtToEquity);
            }
        }
    }
}
