package com.trust.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * إعدادات مرجعية لكل تصنيف نشاط - تُستخدم في حساب مؤشر صحة الأعمال والتوصيات.
 * قابلة للتعديل من لوحة الأدمن.
 */
@Entity
@Table(name = "category_benchmarks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryBenchmark {

    @Id
    @Enumerated(EnumType.STRING)
    private Category category;

    /** هامش الربح المرجعي المتوقع % (مثال: 20.0 يعني 20%) */
    private double targetMarginPercent;

    /** نطاق نسبة السيولة الصحية (Current Ratio) */
    private double liquidityRatioMin;
    private double liquidityRatioMax;

    /** نطاق أشهر تغطية المخزون الصحي */
    private double inventoryCoverageMinMonths;
    private double inventoryCoverageMaxMonths;

    /** عدد الأيام بدون بيع لاعتبار الصنف راكدًا */
    private int stagnationDaysThreshold = 60;
    private int slowMovingDaysThreshold = 30;
    private int mediumMovingDaysThreshold = 14;

    /** أوزان مكونات مؤشر صحة الأعمال (تجمع إلى 100) */
    private double weightSales = 16.7;
    private double weightProfit = 16.7;
    private double weightPricing = 16.6;
    private double weightPurchases = 16.7;
    private double weightInventory = 16.7;
    private double weightLiquidity = 16.6;
}
