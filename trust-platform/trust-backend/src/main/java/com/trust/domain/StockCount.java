package com.trust.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * نتيجة جرد فعلي لصنف واحد: الكمية المعدودة مقابل الكمية الدفترية. تفتح مؤشر دقة
 * الجرد في BHI، وهو أيضًا أداة تشغيلية مفيدة بذاتها لاكتشاف الفروقات.
 */
@Entity
@Table(name = "stock_counts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockCount {

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
    private LocalDate countDate;

    /** الكمية الدفترية لحظة الجرد */
    @Column(nullable = false)
    private double expectedQuantity;

    /** الكمية المعدودة فعليًا على الرف */
    @Column(nullable = false)
    private double countedQuantity;

    private String note;

    public double getDiscrepancy() {
        return countedQuantity - expectedQuantity;
    }
}
