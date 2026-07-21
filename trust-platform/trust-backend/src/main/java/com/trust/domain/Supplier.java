package com.trust.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/** ملف مورّد - يغذّي محرك قرار الشراء بمدة التوريد ومدة الائتمان والتقييم. */
@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String name;

    private String contactInfo;

    /** بريد المورّد - يُستخدم لربط حساب بوابة المورد (دور SUPPLIER) بسجلات هذا المورد عبر كل المؤسسات */
    private String email;

    /** عدد الأيام بين إصدار أمر الشراء واستلام البضاعة */
    @Column(nullable = false)
    private int leadTimeDays = 5;

    /** مدة الائتمان الممنوحة بالأيام (0 يعني دفع فوري) */
    @Column(nullable = false)
    private int creditTermsDays = 0;

    /** تقييم أداء المورد من 100 (سعر، جودة، التزام...) - يُدخل يدويًا في هذه المرحلة */
    @Column(nullable = false)
    private double rating = 80.0;

    private LocalDateTime createdAt = LocalDateTime.now();
}
