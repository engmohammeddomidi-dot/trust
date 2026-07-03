package com.trust.web;

import com.trust.config.AuthenticatedUser;
import com.trust.config.TenantAccessGuard;
import com.trust.service.AuditLogService;
import com.trust.service.ItemService;
import com.trust.web.dto.BulkImportItemsRequest;
import com.trust.web.dto.BulkImportItemsResponse;
import com.trust.web.dto.ItemCreateRequest;
import com.trust.web.dto.ItemDto;
import com.trust.web.dto.ItemLinkSupplierRequest;
import com.trust.repository.ItemRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;
    private final ItemRepository itemRepository;
    private final TenantAccessGuard accessGuard;
    private final AuditLogService auditLogService;

    public ItemController(ItemService itemService, ItemRepository itemRepository,
                           TenantAccessGuard accessGuard, AuditLogService auditLogService) {
        this.itemService = itemService;
        this.itemRepository = itemRepository;
        this.accessGuard = accessGuard;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<ItemDto> list(@RequestParam Long branchId, @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireBranch(principal, branchId);
        return itemService.listByBranch(branchId);
    }

    @GetMapping("/needing-attention")
    public List<ItemDto> needingAttention(@RequestParam Long branchId, @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireBranch(principal, branchId);
        return itemService.listNeedingAttention(branchId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemDto create(@Valid @RequestBody ItemCreateRequest request, @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireBranch(principal, request.branchId());
        ItemDto created = itemService.create(request);
        auditLogService.record(principal.organizationId(), principal.email(), "CREATE_ITEM", "Item",
                String.valueOf(created.id()), "name=" + created.name());
        return created;
    }

    /** استيراد جماعي للأصناف من CSV - يزيل أكبر عائق أمام إدخال مخزون حقيقي بحجم كبير */
    @PostMapping("/bulk")
    public BulkImportItemsResponse bulkImport(@Valid @RequestBody BulkImportItemsRequest request,
                                               @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireBranch(principal, request.branchId());

        int created = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < request.items().size(); i++) {
            var row = request.items().get(i);
            String rowLabel = "سطر " + (i + 1) + " (" + (row.name() == null ? "" : row.name()) + ")";
            if (row.name() == null || row.name().isBlank()) {
                errors.add(rowLabel + ": اسم الصنف مطلوب");
                continue;
            }
            if (!(row.costPrice() > 0)) {
                errors.add(rowLabel + ": سعر التكلفة يجب أن يكون أكبر من صفر");
                continue;
            }
            if (!(row.salePrice() > 0)) {
                errors.add(rowLabel + ": سعر البيع يجب أن يكون أكبر من صفر");
                continue;
            }
            if (!(row.quantity() > 0)) {
                errors.add(rowLabel + ": الكمية يجب أن تكون أكبر من صفر");
                continue;
            }
            try {
                ItemCreateRequest itemRequest = new ItemCreateRequest(
                        request.branchId(), row.name(), row.subCategory(), row.costPrice(),
                        row.salePrice(), row.quantity(), row.lastSaleDate(), row.expiryDate()
                );
                itemService.create(itemRequest);
                created++;
            } catch (Exception e) {
                errors.add(rowLabel + ": " + e.getMessage());
            }
        }

        auditLogService.record(principal.organizationId(), principal.email(), "BULK_IMPORT_ITEMS", "Item", null,
                "created=" + created + ", errors=" + errors.size());
        return new BulkImportItemsResponse(created, errors);
    }

    /** يربط صنفًا بمورّد مفضّل - يغذّي محرك قرار الشراء بمدة توريد حقيقية بدل الافتراضية */
    @PatchMapping("/{id}/supplier")
    public ItemDto linkSupplier(@PathVariable Long id, @Valid @RequestBody ItemLinkSupplierRequest request,
                                 @AuthenticationPrincipal AuthenticatedUser principal) {
        var item = itemRepository.findById(id).orElseThrow(() -> new java.util.NoSuchElementException("الصنف غير موجود"));
        accessGuard.requireBranchOwnership(principal, item.getBranch());
        ItemDto updated = itemService.linkSupplier(id, request);
        auditLogService.record(principal.organizationId(), principal.email(), "LINK_ITEM_SUPPLIER", "Item",
                String.valueOf(id), "supplierId=" + request.supplierId());
        return updated;
    }
}
