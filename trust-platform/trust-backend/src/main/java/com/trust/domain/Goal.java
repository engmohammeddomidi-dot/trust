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

    /**
     * الركائز الاستراتيجية الثلاث. رؤية المنتج تسمّي تبسيط الأهداف إلى ثلاث ركائز
     * "أهم قرار تبسيط" - سبعة مؤشرات متساوية الظهور توحي بسبع روافع، وأكثرها لا يحرّك
     * شيئًا بعد.
     */
    public enum Pillar {
        PROFITABILITY("تعظيم الربحية"),
        WORKING_CAPITAL("كفاءة رأس المال العامل"),
        OPERATIONAL_EFFICIENCY("الكفاءة التشغيلية");

        private final String labelAr;

        Pillar(String labelAr) {
            this.labelAr = labelAr;
        }

        public String getLabelAr() {
            return labelAr;
        }
    }

    /**
     * كل هدف يعلن ركيزته وما إذا كان يؤثّر فعلًا في محرك القرار.
     *
     * اثنان فقط يصلان إلى المحرك اليوم (منع نفاد المخزون، تحسين السيولة). الخمسة
     * الباقية مخزَّنة وغير مؤثّرة لأن محركات التحليل التي كانت ستستهلكها لم تُبنَ بعد.
     * نعلن ذلك في الواجهة بدل عرض سبع روافع متساوية - ورؤية المنتج و PROGRESS.md
     * يتفقان صراحةً على عدم ربط أرقام بمحركات غير موجودة.
     */
    public enum Type {
        INCREASE_PROFITABILITY("زيادة الربحية", Pillar.PROFITABILITY, false),
        IMPROVE_LIQUIDITY("تحسين السيولة", Pillar.WORKING_CAPITAL, true),
        PREVENT_STOCKOUTS("منع نفاد المخزون", Pillar.WORKING_CAPITAL, true),
        REDUCE_STAGNANT_INVENTORY("تقليل المخزون الراكد", Pillar.WORKING_CAPITAL, false),
        INCREASE_SALES("زيادة المبيعات", Pillar.PROFITABILITY, false),
        IMPROVE_SUPPLIER_PERFORMANCE("تحسين أداء الموردين", Pillar.OPERATIONAL_EFFICIENCY, false),
        INCREASE_INVENTORY_TURNOVER("رفع دوران المخزون", Pillar.OPERATIONAL_EFFICIENCY, false);

        private final String labelAr;
        private final Pillar pillar;
        private final boolean influencesEngine;

        Type(String labelAr, Pillar pillar, boolean influencesEngine) {
            this.labelAr = labelAr;
            this.pillar = pillar;
            this.influencesEngine = influencesEngine;
        }

        public String getLabelAr() {
            return labelAr;
        }

        public Pillar getPillar() {
            return pillar;
        }

        /** هل تُغيّر أولوية هذا الهدف سلوك أي محرك فعليًا؟ */
        public boolean influencesEngine() {
            return influencesEngine;
        }
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
