package com.trust.service;

import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;

/**
 * طابور موحَّد للفرص والمخاطر - "ماذا أفعل اليوم؟" في سطر واحد مرتَّب.
 *
 * اليوم كل إشارة تعيش في شاشتها المنفصلة (قرارات الشراء، الأصناف الراكدة، قرب انتهاء
 * الصلاحية)، فيضطر صاحب المحل للتنقّل بين الشاشات ليكتشف ما يستحق وقته. هذه الخدمة
 * ترتيب فوق إشارات موجودة أصلًا - ليست محرك تحليل جديدًا.
 *
 * السقف مقصود: رؤية المنتج تنصّ على ألا تتجاوز الشاشة الرئيسية خمسة عناصر، لأن
 * "خمسين تنبيهًا" تعني أن المستخدم يغلق التطبيق.
 */
@Service
public class OpportunityFeedService {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0");

    /** 🔴 خطر يجب تفاديه، 🟢 فرصة تستحق الاقتناص - نفس تصنيف Decision.Category */
    public enum SignalKind { RISK, OPPORTUNITY }

    /**
     * @param expectedImpact الأثر المالي المتوقَّع بالشيكل
     * @param urgency معامل الإلحاح من 0 إلى 1 - يُضرب في الأثر لترتيب الطابور
     */
    public record Signal(
            SignalKind kind,
            String title,
            String detail,
            double expectedImpact,
            double urgency,
            String suggestedAction,
            Long itemId
    ) {
        public double score() {
            return expectedImpact * urgency;
        }
    }

    /** يرتّب بالأثر المتوقَّع مرجَّحًا بالإلحاح، ثم يقصّ عند السقف */
    public List<Signal> rank(List<Signal> signals, int limit) {
        return signals.stream()
                .sorted(Comparator.comparingDouble(Signal::score).reversed())
                .limit(limit)
                .toList();
    }

    /**
     * رأس مال مجمَّد في صنف راكد. مصنَّف فرصة لا خطرًا: المال موجود ويمكن تحريره -
     * وهذا بالضبط ما تصفه الرؤية بتحويل الخطر إلى فرصة عبر الشراء الجماعي أو التصريف.
     */
    public Signal stagnantStockSignal(String itemName, double tiedUpCapital, Long itemId) {
        return new Signal(
                SignalKind.OPPORTUNITY,
                "رأس مال مجمَّد في %s".formatted(itemName),
                "%s شيكل عالقة في مخزون لا يتحرك".formatted(MONEY.format(tiedUpCapital)),
                tiedUpCapital,
                0.5,
                "اعرضه في طلب جماعي أو حملة تصريف لتحرير السيولة",
                itemId);
    }

    /**
     * بضاعة تقترب من انتهاء الصلاحية. الإلحاح يرتفع كلما اقترب التاريخ - صنف يتبقى له
     * يومان ليس كصنف يتبقى له شهر، ولو تساوت قيمتهما.
     */
    public Signal expirySignal(String itemName, double valueAtRisk, int daysToExpiry, Long itemId) {
        double urgency = 1.0 / (1.0 + Math.max(0, daysToExpiry));
        return new Signal(
                SignalKind.RISK,
                "%s يقترب من انتهاء الصلاحية".formatted(itemName),
                "%s شيكل معرّضة للتلف خلال %d يوم".formatted(MONEY.format(valueAtRisk), daysToExpiry),
                valueAtRisk,
                urgency,
                "خفّض السعر أو اعرضه في طلب جماعي قبل فوات الأوان",
                itemId);
    }

    /** قرار شراء مفتوح - الإشارة الوحيدة التي تملك بالفعل شاشتها وبطاقتها الكاملة */
    public Signal purchaseDecisionSignal(String itemName, double financialImpact,
                                         boolean isRisk, Long itemId) {
        return new Signal(
                isRisk ? SignalKind.RISK : SignalKind.OPPORTUNITY,
                isRisk ? "%s على وشك النفاد".formatted(itemName)
                       : "فرصة إعادة طلب %s".formatted(itemName),
                "%s شيكل ربح معرَّض للضياع".formatted(MONEY.format(financialImpact)),
                financialImpact,
                isRisk ? 1.0 : 0.6,
                "راجع القرار واعتمده أو عدّله",
                itemId);
    }
}
