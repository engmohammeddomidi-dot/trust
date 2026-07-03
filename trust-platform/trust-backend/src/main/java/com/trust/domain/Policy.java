package com.trust.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * سياسات قابلة للتعديل لكل مؤسسة - تحكم محرك القرار بدل ثوابت مبرمجة، كما طلبت
 * رؤية PM ("طبقة السياسات" فوق محركات التحليل). سطر واحد لكل مؤسسة (مثل CategoryBenchmark).
 */
@Entity
@Table(name = "policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false, unique = true)
    private Organization organization;

    /** لا تتجاوز قيمة طلبية شراء واحدة هذه النسبة من السيولة المتاحة حاليًا */
    @Column(nullable = false)
    private double maxPurchaseLiquidityRatio = 0.25;

    /** لا يُعتمد على مورّد تقييمه أقل من هذا الحد دون تنبيه صريح للمستخدم */
    @Column(nullable = false)
    private double minSupplierRating = 0.0;
}
