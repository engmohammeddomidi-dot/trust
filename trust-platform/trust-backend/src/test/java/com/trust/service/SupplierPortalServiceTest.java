package com.trust.service;

import com.trust.domain.*;
import com.trust.repository.PurchaseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ردّ المورّد على أمر شراء (قبول/رفض).
 *
 * الحساسية الأمنية هنا مختلفة عن باقي البوابة: شاشة النظرة العامة تقتصر تلقائيًا على
 * بريد المستخدم فلا تحتاج حارسًا، أما هذا المسار فيستقبل معرّف أمر شراء من العميل -
 * أي أن الخاصية التي كانت تحمي البوابة تسقط، ويجب التحقق صراحةً من أن الأمر يخص
 * المورّد صاحب البريد المسجَّل دخوله.
 */
class SupplierPortalServiceTest {

    private final PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final SupplierPortalService service =
            new SupplierPortalService(purchaseRepository, notificationService);

    private Purchase purchaseOwnedBy(String supplierEmail, Purchase.Status status) {
        Organization org = new Organization();
        org.setId(7L);
        org.setName("سوبرماركت النجمة");

        Branch branch = new Branch();
        branch.setId(3L);
        branch.setName("الفرع الرئيسي");
        branch.setOrganization(org);

        Supplier supplier = new Supplier();
        supplier.setId(11L);
        supplier.setName("شركة الأمين");
        supplier.setEmail(supplierEmail);
        supplier.setOrganization(org);

        Item item = new Item();
        item.setId(5L);
        item.setName("أرز بسمتي");

        Purchase p = new Purchase();
        p.setId(99L);
        p.setBranch(branch);
        p.setItem(item);
        p.setSupplier(supplier);
        p.setQuantity(200);
        p.setCostPrice(22);
        p.setPurchaseDate(LocalDate.now());
        p.setStatus(status);
        return p;
    }

    // ---------- الحدود الأمنية ----------

    @Test
    void aSupplierCannotRespondToAnotherSuppliersOrder() {
        when(purchaseRepository.findById(99L))
                .thenReturn(Optional.of(purchaseOwnedBy("other@supplier.com", Purchase.Status.SENT)));

        assertThatThrownBy(() -> service.accept(99L, "me@supplier.com", null))
                .isInstanceOf(AccessDeniedException.class);

        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void anOrderWithNoSupplierAttached_cannotBeRespondedTo() {
        Purchase orphan = purchaseOwnedBy("me@supplier.com", Purchase.Status.SENT);
        orphan.setSupplier(null);
        when(purchaseRepository.findById(99L)).thenReturn(Optional.of(orphan));

        assertThatThrownBy(() -> service.accept(99L, "me@supplier.com", null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void emailMatchingIgnoresCase() {
        when(purchaseRepository.findById(99L))
                .thenReturn(Optional.of(purchaseOwnedBy("Me@Supplier.com", Purchase.Status.SENT)));
        when(purchaseRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Purchase result = service.accept(99L, "me@supplier.com", null);

        assertThat(result.getSupplierResponse()).isEqualTo(Purchase.SupplierResponse.ACCEPTED);
    }

    // ---------- القبول ----------

    @Test
    void acceptingRecordsTheResponseAndTheDateItWasGiven() {
        when(purchaseRepository.findById(99L))
                .thenReturn(Optional.of(purchaseOwnedBy("me@supplier.com", Purchase.Status.SENT)));
        when(purchaseRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Purchase result = service.accept(99L, "me@supplier.com", LocalDate.now().plusDays(4));

        assertThat(result.getSupplierResponse()).isEqualTo(Purchase.SupplierResponse.ACCEPTED);
        assertThat(result.getSupplierRespondedAt()).isEqualTo(LocalDate.now());
        assertThat(result.getSupplierPromisedDate()).isEqualTo(LocalDate.now().plusDays(4));
    }

    /**
     * القبول لا يغيّر حالة الأمر: تبقى SENT حتى يؤكّد المشتري الاستلام فعليًا. خلط
     * الأمرين كان سيجعل موافقة المورّد تُحدِث المخزون دون وصول بضاعة.
     */
    @Test
    void acceptingDoesNotMarkTheOrderReceived() {
        when(purchaseRepository.findById(99L))
                .thenReturn(Optional.of(purchaseOwnedBy("me@supplier.com", Purchase.Status.SENT)));
        when(purchaseRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Purchase result = service.accept(99L, "me@supplier.com", null);

        assertThat(result.getStatus()).isEqualTo(Purchase.Status.SENT);
        assertThat(result.getReceivedQuantity()).isNull();
    }

    @Test
    void acceptingNotifiesTheBuyingOrganization() {
        when(purchaseRepository.findById(99L))
                .thenReturn(Optional.of(purchaseOwnedBy("me@supplier.com", Purchase.Status.SENT)));
        when(purchaseRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.accept(99L, "me@supplier.com", null);

        verify(notificationService).notify(eq(7L), anyString(), anyString(), eq(Notification.Severity.SUCCESS));
    }

    // ---------- الرفض ----------

    @Test
    void rejectingRecordsTheReasonSoTheBuyerKnowsWhy() {
        when(purchaseRepository.findById(99L))
                .thenReturn(Optional.of(purchaseOwnedBy("me@supplier.com", Purchase.Status.SENT)));
        when(purchaseRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Purchase result = service.reject(99L, "me@supplier.com", "الكمية غير متوفرة حاليًا");

        assertThat(result.getSupplierResponse()).isEqualTo(Purchase.SupplierResponse.REJECTED);
        assertThat(result.getSupplierRejectionReason()).isEqualTo("الكمية غير متوفرة حاليًا");
    }

    @Test
    void rejectingNotifiesTheBuyingOrganizationAsAWarning() {
        when(purchaseRepository.findById(99L))
                .thenReturn(Optional.of(purchaseOwnedBy("me@supplier.com", Purchase.Status.SENT)));
        when(purchaseRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.reject(99L, "me@supplier.com", "نفدت الكمية");

        verify(notificationService).notify(eq(7L), anyString(), anyString(), eq(Notification.Severity.WARNING));
    }

    // ---------- حالات لا يجوز الردّ فيها ----------

    @Test
    void anAlreadyReceivedOrderCannotBeRespondedTo() {
        when(purchaseRepository.findById(99L))
                .thenReturn(Optional.of(purchaseOwnedBy("me@supplier.com", Purchase.Status.RECEIVED)));

        assertThatThrownBy(() -> service.accept(99L, "me@supplier.com", null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void respondingTwiceIsRejected() {
        Purchase already = purchaseOwnedBy("me@supplier.com", Purchase.Status.SENT);
        already.setSupplierResponse(Purchase.SupplierResponse.ACCEPTED);
        when(purchaseRepository.findById(99L)).thenReturn(Optional.of(already));

        assertThatThrownBy(() -> service.reject(99L, "me@supplier.com", "تراجعت"))
                .isInstanceOf(IllegalStateException.class);
    }
}
