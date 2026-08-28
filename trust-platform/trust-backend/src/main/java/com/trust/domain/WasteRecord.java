package com.trust.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * تالف أو فاقد مسجَّل لصنف واحد. يفتح مؤشر نسبة الهدر في BHI، ويغذّي أيضًا فكرة
 * "تحويل الخطر إلى فرصة" في رؤية المنتج (بضاعة قاربت الانتهاء).
 */
@Entity
@Table(name = "waste_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WasteRecord {

    public enum Reason { EXPIRY, DAMAGE, THEFT, OTHER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private LocalDate wasteDate;

    @Column(nullable = false)
    private double quantity;

    /** تكلفة الوحدة لحظة التسجيل - تُثبَّت حتى لا يتغيّر التاريخ بتغيّر سعر الصنف لاحقًا */
    @Column(nullable = false)
    private double unitCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Reason reason = Reason.OTHER;

    private String note;

    public double getTotalCost() {
        return quantity * unitCost;
    }
}
