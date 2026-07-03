package com.trust.service;

import com.trust.domain.Item;

/**
 * تقدير مبسط لمتوسط المبيعات اليومية للصنف بناءً على حالة حركته (MVP بدون سجل
 * مبيعات مفصّل لكل صنف). مشترك بين محرك التوصيات ومحرك قرار الشراء حتى لا يختلف
 * التقدير بين الاثنين لنفس الصنف.
 */
final class SalesEstimator {

    private SalesEstimator() {
    }

    static double estimateDailySales(Item item) {
        return switch (item.getMovementStatus()) {
            case FAST -> Math.max(1, item.getQuantity() * 0.15);
            case MEDIUM -> Math.max(0.5, item.getQuantity() * 0.07);
            case SLOW -> Math.max(0.2, item.getQuantity() * 0.03);
            case STAGNANT -> 0.0;
        };
    }
}
