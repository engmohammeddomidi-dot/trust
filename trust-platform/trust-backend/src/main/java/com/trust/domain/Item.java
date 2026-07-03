package com.trust.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Item {

    public enum MovementStatus { FAST, MEDIUM, SLOW, STAGNANT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(nullable = false)
    private String name;

    private String subCategory;

    @Column(nullable = false)
    private double costPrice;

    @Column(nullable = false)
    private double salePrice;

    @Column(nullable = false)
    private double quantity;

    private LocalDate lastSaleDate;

    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    private MovementStatus movementStatus = MovementStatus.FAST;

    /** المورّد المفضّل لهذا الصنف - يغذّي محرك قرار الشراء بمدة التوريد الفعلية */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    /** عدد أيام مخزون الأمان المطلوبة فوق مدة التوريد قبل اعتبار الصنف بحاجة لإعادة طلب */
    @Column(nullable = false)
    private int safetyStockDays = 3;

    /** هامش ربح الصنف % */
    public double getMarginPercent() {
        if (salePrice <= 0) return 0;
        return ((salePrice - costPrice) / salePrice) * 100.0;
    }

    /** القيمة الإجمالية للمخزون من هذا الصنف بسعر التكلفة */
    public double getInventoryValue() {
        return costPrice * quantity;
    }
}
