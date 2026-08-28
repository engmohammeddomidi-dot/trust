package com.trust.web;

import com.trust.config.AuthenticatedUser;
import com.trust.domain.Purchase;
import com.trust.domain.Supplier;
import com.trust.domain.User;
import com.trust.repository.PurchaseRepository;
import com.trust.repository.SupplierRepository;
import com.trust.repository.UserRepository;
import com.trust.web.dto.SupplierPortalOverviewDto;
import com.trust.web.dto.SupplierPortalPurchaseDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.trust.service.SupplierPortalService;
import com.trust.web.dto.SupplierOrderActionDto;
import com.trust.web.dto.SupplierOrderResponseRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * بوابة المورّد - القسم الجديد: مورّد واحد قد يتعامل مع عدة مؤسسات مستأجرة، لذا لا يُستخدم
 * TenantAccessGuard هنا (مصمم لعزل مؤسسة واحدة). الربط عبر بريد المورّد بدل معرّف مؤسسة:
 * كل استعلام يقتصر تلقائيًا على سجلات Supplier التي بريدها يطابق بريد المستخدم المسجّل دخوله
 * (principal.email())، وهذا وحده كافٍ لمنع أي تسريب بين المؤسسات دون الحاجة لحارس إضافي.
 */
@RestController
@RequestMapping("/api/supplier")
public class SupplierPortalController {

    private final SupplierRepository supplierRepository;
    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final SupplierPortalService portalService;

    public SupplierPortalController(SupplierRepository supplierRepository, PurchaseRepository purchaseRepository,
                                     UserRepository userRepository, SupplierPortalService portalService) {
        this.supplierRepository = supplierRepository;
        this.purchaseRepository = purchaseRepository;
        this.userRepository = userRepository;
        this.portalService = portalService;
    }

    /**
     * قبول أمر شراء. يسجّل التزام المورّد ويُخطر المؤسسة المشترية، ولا يغيّر حالة
     * الأمر - تبقى SENT حتى يؤكّد المشتري الاستلام فعليًا.
     */
    @PatchMapping("/orders/{purchaseId}/accept")
    public SupplierOrderActionDto accept(@PathVariable Long purchaseId,
                                         @RequestBody(required = false) SupplierOrderResponseRequest request,
                                         @AuthenticationPrincipal AuthenticatedUser principal) {
        LocalDate promised = request != null && request.promisedDate() != null
                ? LocalDate.parse(request.promisedDate()) : null;
        Purchase saved = portalService.accept(purchaseId, principal.email(), promised);
        return toActionDto(saved);
    }

    @PatchMapping("/orders/{purchaseId}/reject")
    public SupplierOrderActionDto reject(@PathVariable Long purchaseId,
                                         @RequestBody(required = false) SupplierOrderResponseRequest request,
                                         @AuthenticationPrincipal AuthenticatedUser principal) {
        Purchase saved = portalService.reject(purchaseId, principal.email(),
                request != null ? request.reason() : null);
        return toActionDto(saved);
    }

    private static SupplierOrderActionDto toActionDto(Purchase p) {
        return new SupplierOrderActionDto(p.getId(), p.getSupplierResponse().name(),
                p.getSupplierRespondedAt(), p.getSupplierPromisedDate(), p.getSupplierRejectionReason());
    }

    @GetMapping("/overview")
    public SupplierPortalOverviewDto overview(@AuthenticationPrincipal AuthenticatedUser principal) {
        List<Supplier> linkedSuppliers = supplierRepository.findByEmailIgnoreCase(principal.email());
        User user = userRepository.findById(principal.userId()).orElseThrow();

        if (linkedSuppliers.isEmpty()) {
            return new SupplierPortalOverviewDto(user.getName(), 0, 0, 0, 0, 0, null, List.of());
        }

        Map<Long, Supplier> supplierById = linkedSuppliers.stream()
                .collect(Collectors.toMap(Supplier::getId, s -> s));
        List<Long> supplierIds = linkedSuppliers.stream().map(Supplier::getId).toList();

        List<Purchase> purchases = purchaseRepository.findBySupplierIdInOrderByPurchaseDateDesc(supplierIds);

        int openOrdersCount = 0;
        double openOrdersValue = 0;
        int receivedOrdersCount = 0;
        double totalReceivedValue = 0;
        for (Purchase p : purchases) {
            if (p.getStatus() == Purchase.Status.SENT) {
                openOrdersCount++;
                openOrdersValue += p.getTotalCost();
            } else {
                receivedOrdersCount++;
                totalReceivedValue += p.getTotalCost();
            }
        }

        double avgRating = linkedSuppliers.stream().mapToDouble(Supplier::getRating).average().orElse(0);
        long organizationsServed = linkedSuppliers.stream()
                .map(s -> s.getOrganization().getId()).distinct().count();

        List<SupplierPortalPurchaseDto> recentOrders = purchases.stream()
                .limit(10)
                .map(p -> {
                    Supplier supplier = supplierById.get(p.getSupplier().getId());
                    return new SupplierPortalPurchaseDto(
                            p.getId(),
                            p.getBranch().getOrganization().getName(),
                            p.getBranch().getName(),
                            p.getItem() != null ? p.getItem().getName() : null,
                            p.getQuantity(),
                            p.getCostPrice(),
                            p.getStatus().name(),
                            p.getPurchaseDate(),
                            p.getPurchaseDate().plusDays(supplier.getLeadTimeDays()),
                            p.getReceivedDate(),
                            p.getSupplierResponse().name(),
                            p.getSupplierPromisedDate(),
                            p.getSupplierRejectionReason());
                })
                .toList();

        return new SupplierPortalOverviewDto(linkedSuppliers.get(0).getName(), (int) organizationsServed,
                openOrdersCount, openOrdersValue, receivedOrdersCount, totalReceivedValue,
                Math.round(avgRating * 10) / 10.0, recentOrders);
    }
}
