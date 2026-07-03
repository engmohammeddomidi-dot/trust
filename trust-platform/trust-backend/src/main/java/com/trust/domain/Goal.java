package com.trust.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * أولوية عمل مرتّبة تحدّدها المؤسسة (من رؤية PM: "طبقة الأهداف") - تُستخدم لتكييف
 * سلوك محركات القرار بدل تطبيق نفس السياسة على الجميع. priority من 1 (أقل أهمية)
 * إلى 5 (أهم أولوية)، القيمة الافتراضية 3 = محايدة.
 */
@Entity
@Table(name = "goals", uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Goal {

    public enum Type {
        INCREASE_PROFITABILITY, IMPROVE_LIQUIDITY, PREVENT_STOCKOUTS, REDUCE_STAGNANT_INVENTORY,
        INCREASE_SALES, IMPROVE_SUPPLIER_PERFORMANCE, INCREASE_INVENTORY_TURNOVER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false)
    private int priority = 3;
}
