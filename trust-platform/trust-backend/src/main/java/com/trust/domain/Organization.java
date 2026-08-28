package com.trust.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * حقوق الملكية - يُدخلها صاحب المنشأة في الإعدادات. فارغة تعني أن مؤشر نسبة الدين
     * إلى حقوق الملكية يبقى "غير متاح" بصدق بدل افتراض رقم.
     */
    private Double equity;
}
