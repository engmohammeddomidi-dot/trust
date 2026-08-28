package com.trust.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * بند مصروف تشغيلي شهري لفرع واحد. يفتح المؤشرين الوحيدين المتبقيين في محور الربحية
 * (هامش صافي الربح ونسبة المصاريف) - وهو المحور الأثقل وزنًا في BHI (0.30).
 *
 * الكمية × القيمة تحاكي جدول المصاريف في نموذج مدير المنتج حرفيًا (ثلاثة موظفي رف
 * بألفين لكلٍّ = 6000)، بدل إجبار المستخدم على ضرب الأرقام ذهنيًا.
 */
@Entity
@Table(name = "monthly_expenses",
        uniqueConstraints = @UniqueConstraint(columnNames = {"branch_id", "expense_month", "category"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    /** أول يوم في الشهر - يمثّل الشهر كاملًا */
    @Column(nullable = false)
    private LocalDate expenseMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategory category;

    @Column(nullable = false)
    private double unitAmount;

    @Column(nullable = false)
    private int quantity = 1;

    private String note;

    public double getTotal() {
        return unitAmount * quantity;
    }
}
