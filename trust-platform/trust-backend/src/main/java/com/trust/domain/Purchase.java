package com.trust.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

/** سجل عملية شراء من مورد - القسم 7.4 و 7.10 من خطة MVP */
@Entity
@Table(name = "purchases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Purchase {

    /**
     * SENT: أمر شراء صادر (عادة عن اعتماد Decision) بانتظار الاستلام - لا يزال لا يُحتسب في المخزون.
     * RECEIVED: تم استلام البضاعة فعليًا - القيمة الافتراضية للإدخال اليدوي القديم (يسجّل شراء مكتمل بالفعل).
     */
    public enum Status { SENT, RECEIVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    /** المورد الحقيقي إن وُجد - قد تكون فارغة للإدخالات اليدوية القديمة التي تعتمد على supplierName فقط */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    /** القرار الذي أنشأ هذه الطلبية - فارغ للإدخالات اليدوية المباشرة عبر صفحة المشتريات */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decision_id")
    private Decision decision;

    @Column(nullable = false)
    private String supplierName;

    @Column(nullable = false)
    private double quantity;

    @Column(nullable = false)
    private double costPrice;

    @Column(nullable = false)
    private LocalDate purchaseDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.RECEIVED;

    private Double receivedQuantity;
    private LocalDate receivedDate;
    private Boolean priceMatched;
    private boolean hasDamage;

    /** true إن اختلفت الكمية المستلمة عن المطلوبة، أو لم يتطابق السعر، أو وُجد تلف */
    private boolean hasDiscrepancy;

    public double getTotalCost() {
        return quantity * costPrice;
    }
}
