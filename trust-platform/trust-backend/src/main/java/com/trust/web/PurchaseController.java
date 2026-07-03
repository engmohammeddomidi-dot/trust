package com.trust.web;

import com.trust.config.AuthenticatedUser;
import com.trust.config.TenantAccessGuard;
import com.trust.domain.Branch;
import com.trust.domain.Item;
import com.trust.domain.Purchase;
import com.trust.repository.ItemRepository;
import com.trust.repository.PurchaseRepository;
import com.trust.service.AuditLogService;
import com.trust.service.DecisionActionService;
import com.trust.web.dto.PurchaseCreateRequest;
import com.trust.web.dto.PurchaseDto;
import com.trust.web.dto.ReceivePurchaseRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseRepository purchaseRepository;
    private final ItemRepository itemRepository;
    private final TenantAccessGuard accessGuard;
    private final AuditLogService auditLogService;
    private final DecisionActionService decisionActionService;

    public PurchaseController(PurchaseRepository purchaseRepository, ItemRepository itemRepository,
                               TenantAccessGuard accessGuard, AuditLogService auditLogService,
                               DecisionActionService decisionActionService) {
        this.purchaseRepository = purchaseRepository;
        this.itemRepository = itemRepository;
        this.accessGuard = accessGuard;
        this.auditLogService = auditLogService;
        this.decisionActionService = decisionActionService;
    }

    @GetMapping
    public List<PurchaseDto> list(@RequestParam Long branchId, @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireBranch(principal, branchId);
        return purchaseRepository.findByBranchIdOrderByPurchaseDateDesc(branchId).stream().map(PurchaseController::toDto).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseDto create(@Valid @RequestBody PurchaseCreateRequest request, @AuthenticationPrincipal AuthenticatedUser principal) {
        Branch branch = accessGuard.requireBranch(principal, request.branchId());

        Item item = null;
        if (request.itemId() != null) {
            item = itemRepository.findById(request.itemId()).orElseThrow();
            if (!item.getBranch().getId().equals(branch.getId())) {
                throw new AccessDeniedException("الصنف لا يتبع لهذا الفرع");
            }
        }

        Purchase purchase = new Purchase();
        purchase.setBranch(branch);
        purchase.setItem(item);
        purchase.setSupplierName(request.supplierName());
        purchase.setQuantity(request.quantity());
        purchase.setCostPrice(request.costPrice());
        purchase.setPurchaseDate(request.purchaseDate());

        Purchase saved = purchaseRepository.save(purchase);
        auditLogService.record(principal.organizationId(), principal.email(), "CREATE_PURCHASE", "Purchase",
                saved.getId().toString(), "supplier=" + saved.getSupplierName() + ", total=" + saved.getTotalCost());
        return toDto(saved);
    }

    /** يسجّل استلام طلبية (صادرة عن قرار شراء معتمد) - يحدّث المخزون فعليًا ويقيّم المورد */
    @PatchMapping("/{id}/receive")
    public PurchaseDto receive(@PathVariable Long id, @Valid @RequestBody ReceivePurchaseRequest request,
                                @AuthenticationPrincipal AuthenticatedUser principal) {
        Purchase existing = purchaseRepository.findById(id).orElseThrow();
        accessGuard.requireBranchOwnership(principal, existing.getBranch());
        Purchase received = decisionActionService.receive(id, request.receivedQuantity(), request.priceMatched(), request.hasDamage());
        auditLogService.record(principal.organizationId(), principal.email(), "RECEIVE_PURCHASE", "Purchase",
                String.valueOf(id), "receivedQuantity=" + request.receivedQuantity() + ", discrepancy=" + received.isHasDiscrepancy());
        return toDto(received);
    }

    private static PurchaseDto toDto(Purchase p) {
        return new PurchaseDto(
                p.getId(),
                p.getItem() != null ? p.getItem().getId() : null,
                p.getItem() != null ? p.getItem().getName() : null,
                p.getDecision() != null ? p.getDecision().getId() : null,
                p.getSupplier() != null ? p.getSupplier().getId() : null,
                p.getSupplierName(),
                p.getQuantity(),
                p.getCostPrice(),
                p.getTotalCost(),
                p.getPurchaseDate(),
                p.getStatus().name(),
                p.getReceivedQuantity(),
                p.getReceivedDate(),
                p.getPriceMatched(),
                p.isHasDamage(),
                p.isHasDiscrepancy()
        );
    }
}
