package com.trust.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DecisionDto(
        Long id,
        Long itemId,
        String itemName,
        Long supplierId,
        String supplierName,
        String type,
        String category,
        String status,
        double suggestedQuantity,
        Double approvedQuantity,
        String reasonSummary,
        double confidenceScore,
        double financialImpact,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        String actualOutcome,
        /** "لو تجاهلت" - الوجه الآخر للتوصية */
        String ifIgnoredSummary,
        /** القيود التي راعاها المحرك، مفصولة بنقطة */
        String constraintsSummary,
        /** أسباب درجة الثقة، مفصولة بنقطة */
        String confidenceReasons,
        List<Alternative> alternatives
) {
    /** بديل معروض على البطاقة - واحد منها فقط recommended */
    public record Alternative(
            String key,
            String label,
            double quantity,
            double orderValue,
            double coverageDays,
            boolean recommended,
            boolean liquidityLimited,
            String tradeOff
    ) {}
}
