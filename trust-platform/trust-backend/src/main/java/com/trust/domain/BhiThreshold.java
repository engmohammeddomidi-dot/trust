package com.trust.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * تجاوز صريح لحدود مؤشر واحد ضمن فئة نشاط واحدة. الجدول متفرّق عمدًا: الحدود
 * الافتراضية تعيش في BhiIndicatorCode، ولا يوجد صف هنا إلا حين يغيّر المشرف قيمة
 * فعليًا - نفس نمط CategoryBenchmark، وبلا بذر ثلاثة عشر صفًا لكل فئة.
 */
@Entity
@Table(name = "bhi_thresholds",
        uniqueConstraints = @UniqueConstraint(columnNames = {"category", "code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BhiThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BhiIndicatorCode code;

    @Column(nullable = false)
    private double weakThreshold;

    @Column(nullable = false)
    private double mediumThreshold;

    @Column(nullable = false)
    private double excellentThreshold;
}
