package com.trust.service;

import com.trust.domain.Decision;
import com.trust.domain.Item;
import com.trust.domain.Purchase;
import com.trust.domain.Supplier;
import com.trust.repository.ItemRepository;
import com.trust.repository.PurchaseRepository;
import com.trust.repository.SupplierRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

/**
 * ينفّذ الإجراءات المتاحة على قرار شراء (اعتماد/تعديل/تأجيل/تجاهل) ثم يتابعه حتى
 * الاستلام - يغلق الحلقة الموصوفة في رؤية PM: "الاعتماد لا ينهي العمل، بل تبدأ
 * المتابعة" ثم "بعد الاستلام تبدأ مرحلة التعلّم".
 */
@Service
public class DecisionActionService {

    /** تعديلات تقييم المورد بعد الاستلام - رقمية بسيطة وقابلة للفهم، وليست معادلة معقّدة */
    private static final double RATING_BONUS_PERFECT = 1.0;
    private static final double RATING_PENALTY_QUANTITY_MISMATCH = 3.0;
    private static final double RATING_PENALTY_PRICE_MISMATCH = 2.0;
    private static final double RATING_PENALTY_DAMAGE = 5.0;

    private final PurchaseRepository purchaseRepository;
    private final ItemRepository itemRepository;
    private final SupplierRepository supplierRepository;

    public DecisionActionService(PurchaseRepository purchaseRepository, ItemRepository itemRepository,
                                  SupplierRepository supplierRepository) {
        this.purchaseRepository = purchaseRepository;
        this.itemRepository = itemRepository;
        this.supplierRepository = supplierRepository;
    }

    public Decision approve(Decision decision) {
        decision.setStatus(Decision.Status.APPROVED);
        decision.setApprovedQuantity(decision.getSuggestedQuantity());
        decision.setResolvedAt(LocalDateTime.now());
        issuePurchaseOrder(decision);
        return decision;
    }

    public Decision modify(Decision decision, double quantity, Supplier supplier) {
        if (supplier != null) {
            decision.setSupplier(supplier);
        }
        decision.setStatus(Decision.Status.MODIFIED);
        decision.setApprovedQuantity(quantity);
        decision.setResolvedAt(LocalDateTime.now());
        issuePurchaseOrder(decision);
        return decision;
    }

    public Decision defer(Decision decision) {
        decision.setStatus(Decision.Status.DEFERRED);
        decision.setResolvedAt(LocalDateTime.now());
        return decision;
    }

    public Decision dismiss(Decision decision) {
        decision.setStatus(Decision.Status.DISMISSED);
        decision.setResolvedAt(LocalDateTime.now());
        return decision;
    }

    /** ينشئ أمر شراء (Purchase بحالة SENT) مرتبط بالقرار المعتمد - لا يُحتسب في المخزون حتى الاستلام */
    private void issuePurchaseOrder(Decision decision) {
        Supplier supplier = decision.getSupplier();
        Purchase purchase = new Purchase();
        purchase.setBranch(decision.getBranch());
        purchase.setItem(decision.getItem());
        purchase.setSupplier(supplier);
        purchase.setDecision(decision);
        purchase.setSupplierName(supplier != null ? supplier.getName() : "مورد غير محدد");
        purchase.setQuantity(decision.getApprovedQuantity());
        purchase.setCostPrice(decision.getItem().getCostPrice());
        purchase.setPurchaseDate(LocalDate.now());
        purchase.setStatus(Purchase.Status.SENT);
        purchaseRepository.save(purchase);
    }

    /**
     * يسجّل استلام طلبية: يحدّث المخزون فعليًا ويكتب نتيجة القرار (مرحلة "القياس" و"التعلّم")
     * على القرار المرتبط إن وُجد، ويعدّل تقييم المورد حسب مدى مطابقة الاستلام للمتوقع.
     */
    public Purchase receive(Long purchaseId, double receivedQuantity, boolean priceMatched, boolean hasDamage) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new NoSuchElementException("أمر الشراء غير موجود"));
        if (purchase.getStatus() == Purchase.Status.RECEIVED) {
            throw new IllegalStateException("تم استلام هذه الطلبية مسبقًا");
        }

        boolean quantityMismatch = Math.abs(receivedQuantity - purchase.getQuantity()) > 0.001;
        boolean discrepancy = quantityMismatch || !priceMatched || hasDamage;

        purchase.setStatus(Purchase.Status.RECEIVED);
        purchase.setReceivedQuantity(receivedQuantity);
        purchase.setReceivedDate(LocalDate.now());
        purchase.setPriceMatched(priceMatched);
        purchase.setHasDamage(hasDamage);
        purchase.setHasDiscrepancy(discrepancy);

        Item item = purchase.getItem();
        if (item != null) {
            item.setQuantity(item.getQuantity() + receivedQuantity);
            itemRepository.save(item);
        }

        Decision decision = purchase.getDecision();
        if (decision != null) {
            decision.setActualOutcome(buildOutcomeSummary(purchase, quantityMismatch, priceMatched, hasDamage));
            decision.setOutcomeRecordedAt(LocalDateTime.now());
        }

        Supplier supplier = purchase.getSupplier();
        if (supplier != null) {
            double delta = discrepancy
                    ? -(quantityMismatch ? RATING_PENALTY_QUANTITY_MISMATCH : 0)
                      - (!priceMatched ? RATING_PENALTY_PRICE_MISMATCH : 0)
                      - (hasDamage ? RATING_PENALTY_DAMAGE : 0)
                    : RATING_BONUS_PERFECT;
            supplier.setRating(Math.max(0, Math.min(100, supplier.getRating() + delta)));
            supplierRepository.save(supplier);
        }

        return purchaseRepository.save(purchase);
    }

    private String buildOutcomeSummary(Purchase purchase, boolean quantityMismatch, boolean priceMatched, boolean hasDamage) {
        if (!quantityMismatch && priceMatched && !hasDamage) {
            return String.format("تم الاستلام بالكامل ومطابق للتوقع: %.0f وحدة كما هو متفق عليه.", purchase.getReceivedQuantity());
        }
        StringBuilder sb = new StringBuilder("تم الاستلام مع ملاحظات: ");
        if (quantityMismatch) {
            sb.append(String.format("الكمية المستلمة %.0f تختلف عن المطلوبة %.0f. ", purchase.getReceivedQuantity(), purchase.getQuantity()));
        }
        if (!priceMatched) {
            sb.append("السعر عند الاستلام لم يطابق السعر المتفق عليه. ");
        }
        if (hasDamage) {
            sb.append("وُجد تلف في جزء من الشحنة. ");
        }
        return sb.toString().trim();
    }
}
