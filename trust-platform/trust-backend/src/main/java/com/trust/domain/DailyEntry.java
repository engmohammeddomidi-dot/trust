package com.trust.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

/** الإدخال اليدوي اليومي للمبيعات والربح والسيولة لكل فرع */
@Entity
@Table(name = "daily_entries", uniqueConstraints = @UniqueConstraint(columnNames = {"branch_id", "entryDate"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DailyEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(nullable = false)
    private LocalDate entryDate;

    @Column(nullable = false)
    private double totalSales;

    @Column(nullable = false)
    private double totalCogs;

    /** إجمالي الربح = المبيعات - تكلفة البضاعة المباعة (يمكن إدخاله يدويًا أيضًا) */
    private double totalProfit;

    private double availableLiquidity;

    private double receivables;

    private double payables;

    /** هامش الربح % = (اجمالي الربح / المبيعات) * 100 */
    public double getMarginPercent() {
        if (totalSales <= 0) return 0;
        return (totalProfit / totalSales) * 100.0;
    }
}
