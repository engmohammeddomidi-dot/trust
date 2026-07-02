package com.trust.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/** طلب شراء جماعي من مورد يجمّع طلبات عدة مؤسسات - القسم 8.3 من خطة MVP (Phase 2) */
@Entity
@Table(name = "group_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupOrder {

    public enum Status { COLLECTING, NEGOTIATED, DISTRIBUTED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String itemName;

    @Column(nullable = false)
    private double targetQuantity;

    @Column(nullable = false)
    private double currentQuantity = 0;

    /** السعر الفردي التقديري لو اشترت كل مؤسسة بمفردها - يُستخدم لحساب التوفير بعد التفاوض */
    @Column(nullable = false)
    private double estimatedMarketPrice;

    private Double negotiatedPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.COLLECTING;

    private LocalDateTime createdAt = LocalDateTime.now();
}
