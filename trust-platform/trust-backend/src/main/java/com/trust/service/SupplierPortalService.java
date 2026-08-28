package com.trust.service;

import com.trust.domain.Notification;
import com.trust.domain.Purchase;
import com.trust.domain.Supplier;
import com.trust.repository.PurchaseRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * ردّ المورّد على أوامر الشراء الموجَّهة إليه.
 *
 * ملاحظة أمنية مهمة: شاشة النظرة العامة في البوابة آمنة تلقائيًا لأن كل استعلاماتها
 * مقيَّدة ببريد المستخدم ولا تستقبل أي معرّف من العميل. هذا المسار يكسر تلك الخاصية -
 * فهو يستقبل معرّف أمر شراء - لذا يجب التحقق صراحةً من ملكية الأمر قبل أي تعديل.
 */
@Service
public class SupplierPortalService {

    private final PurchaseRepository purchaseRepository;
    private final NotificationService notificationService;

    public SupplierPortalService(PurchaseRepository purchaseRepository,
                                 NotificationService notificationService) {
        this.purchaseRepository = purchaseRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Purchase accept(Long purchaseId, String supplierEmail, LocalDate promisedDate) {
        Purchase purchase = requireOwnPendingOrder(purchaseId, supplierEmail);

        purchase.setSupplierResponse(Purchase.SupplierResponse.ACCEPTED);
        purchase.setSupplierRespondedAt(LocalDate.now());
        purchase.setSupplierPromisedDate(promisedDate);

        notificationService.notify(
                purchase.getBranch().getOrganization().getId(),
                "المورّد قبل أمر الشراء",
                "%s قبل توريد %s%s".formatted(
                        purchase.getSupplier().getName(),
                        itemLabel(purchase),
                        promisedDate != null ? " ووعد بالتسليم في " + promisedDate : ""),
                Notification.Severity.SUCCESS);

        return purchaseRepository.save(purchase);
    }

    @Transactional
    public Purchase reject(Long purchaseId, String supplierEmail, String reason) {
        Purchase purchase = requireOwnPendingOrder(purchaseId, supplierEmail);

        purchase.setSupplierResponse(Purchase.SupplierResponse.REJECTED);
        purchase.setSupplierRespondedAt(LocalDate.now());
        purchase.setSupplierRejectionReason(reason);

        notificationService.notify(
                purchase.getBranch().getOrganization().getId(),
                "المورّد اعتذر عن أمر الشراء",
                "%s اعتذر عن توريد %s%s".formatted(
                        purchase.getSupplier().getName(),
                        itemLabel(purchase),
                        reason != null && !reason.isBlank() ? " - السبب: " + reason : ""),
                Notification.Severity.WARNING);

        return purchaseRepository.save(purchase);
    }

    /** الأمر موجود، ويخص هذا المورّد، ولم يُردَّ عليه ولم يُستلَم بعد */
    private Purchase requireOwnPendingOrder(Long purchaseId, String supplierEmail) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new AccessDeniedException("أمر الشراء غير متاح"));

        Supplier supplier = purchase.getSupplier();
        // أمر بلا مورّد مرتبط لا يخص أحدًا - نرفضه كرفض ملكية لا كخطأ بيانات، حتى لا
        // يكشف الردّ للمهاجم أي معرّفات موجودة فعلًا
        if (supplier == null || supplier.getEmail() == null
                || !supplier.getEmail().equalsIgnoreCase(supplierEmail)) {
            throw new AccessDeniedException("أمر الشراء غير متاح");
        }

        if (purchase.getStatus() != Purchase.Status.SENT) {
            throw new IllegalStateException("لا يمكن الردّ على أمر تم استلامه بالفعل");
        }
        if (purchase.getSupplierResponse() != Purchase.SupplierResponse.PENDING) {
            throw new IllegalStateException("تم الردّ على هذا الأمر مسبقًا");
        }
        return purchase;
    }

    private String itemLabel(Purchase purchase) {
        return purchase.getItem() != null ? purchase.getItem().getName() : "الطلبية";
    }
}
