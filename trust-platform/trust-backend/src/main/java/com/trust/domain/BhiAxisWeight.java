package com.trust.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * تجاوز صريح لوزن محور واحد ضمن فئة نشاط واحدة. متفرّق مثل BhiThreshold - الأوزان
 * الافتراضية (0.30/0.20/0.20/0.15/0.15) تعيش في BhiAxis.
 */
@Entity
@Table(name = "bhi_axis_weights",
        uniqueConstraints = @UniqueConstraint(columnNames = {"category", "axis"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BhiAxisWeight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BhiAxis axis;

    @Column(nullable = false)
    private double weight;
}
