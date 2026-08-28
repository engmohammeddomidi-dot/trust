package com.trust.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * قرار صادر عن محرك القرارات (Decision Engine) - أول نوع مدعوم هو PURCHASE_ORDER.
 * على عكس Recommendation (توصية بسيطة نص+قيمة)، كل Decision يحمل شرحًا كاملاً
 * قابلاً للتفسير: السبب، درجة الثقة، الأثر المالي، والنتيجة الفعلية بعد التنفيذ
 * (تُستخدم لاحقًا لقياس جودة القرار - حلقة التعلّم في رؤية PM).
 */
@Entity
@Table(name = "decisions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Decision {

    public enum Type { PURCHASE_ORDER }
    public enum Status { OPEN, APPROVED, MODIFIED, DEFERRED, DISMISSED }

    /**
     * تصنيف حقيقي مشتق من إشارات المحرك (وليس افتراضيًا): RISK إذا كان المخزون لن يصمد
     * حتى وصول التوريد (نفاد فعلي محتمل)، OPPORTUNITY إذا كان القرار استباقيًا ولا يزال
     * هناك هامش أمان - يطابق تصنيفَي "الفرص/المخاطر" من رؤية PM.
     */
    public enum Category { RISK, OPPORTUNITY }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    /** المورّد المقترح لتنفيذ القرار - قد يكون فارغًا إن لم يوجد مورّد مرتبط بالصنف بعد */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Enumerated(EnumType.STRING)
    private Status status = Status.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false)
    private double suggestedQuantity;

    /** الكمية الفعلية بعد اعتماد/تعديل المستخدم - فارغة طالما القرار OPEN */
    private Double approvedQuantity;

    /** شرح مقروء لسبب القرار - يُعرض دائمًا للمستخدم (Explainable AI) */
    @Column(nullable = false, length = 1000)
    private String reasonSummary;

    /** درجة الثقة بالتوصية من 100 */
    @Column(nullable = false)
    private double confidenceScore;

    /** الأثر المالي المتوقع (شيكل) - قيمة المخاطرة إن لم يُنفَّذ القرار */
    @Column(nullable = false)
    private double financialImpact;

    /** "لو تجاهلت" - الوجه الآخر للتوصية، بلا هذا السطر تُقرأ البطاقة كإعلان لا كنصيحة */
    @Column(length = 500)
    private String ifIgnoredSummary;

    /** القيود التي راعاها المحرك (سقف السيولة، سياسة المورّد) - مفصولة بنقطة */
    @Column(length = 700)
    private String constraintsSummary;

    /** أسباب درجة الثقة، حتى لا تكون نسبة مجرّدة غير قابلة للمساءلة */
    @Column(length = 700)
    private String confidenceReasons;

    /** البدائل المعروضة على البطاقة، مسلسلة JSON - تُقرأ مع القرار ولا يُستعلَم عنها وحدها */
    @Column(length = 2000)
    private String alternativesJson;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime resolvedAt;

    /** نتيجة القرار الفعلية بعد التنفيذ - تُملأ لاحقًا في مرحلة "القياس" (خارج نطاق هذه الشريحة) */
    private String actualOutcome;
    private LocalDateTime outcomeRecordedAt;
}
