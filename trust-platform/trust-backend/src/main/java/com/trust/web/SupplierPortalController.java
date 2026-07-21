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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    public SupplierPortalController(SupplierRepository supplierRepository, PurchaseRepository purchaseRepository,
                                     UserRepository userRepository) {
        this.supplierRepository = supplierRepository;
        this.purchaseRepository = purchaseRepository;
        this.userRepository = userRepository;
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
                            p.getReceivedDate());
                })
                .toList();

        return new SupplierPortalOverviewDto(linkedSuppliers.get(0).getName(), (int) organizationsServed,
                openOrdersCount, openOrdersValue, receivedOrdersCount, totalReceivedValue,
                Math.round(avgRating * 10) / 10.0, recentOrders);
    }
}
