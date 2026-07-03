package com.trust.web;

import com.trust.config.AuthenticatedUser;
import com.trust.domain.Branch;
import com.trust.domain.Organization;
import com.trust.domain.User;
import com.trust.repository.*;
import com.trust.service.AuditLogService;
import com.trust.web.dto.TenantDataExportDto;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * يفي بحق "تصدير البيانات" المذكور في شروط الاستخدام (TosGateModal) - تصدير شامل
 * لكل بيانات مؤسسة المستخدم الحالي بصيغة JSON واحدة. متاح لصاحب المؤسسة (OWNER) فقط.
 */
@RestController
public class DataExportController {

    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final DailyEntryRepository dailyEntryRepository;
    private final PurchaseRepository purchaseRepository;
    private final RecommendationRepository recommendationRepository;
    private final AuditLogService auditLogService;

    public DataExportController(OrganizationRepository organizationRepository, BranchRepository branchRepository,
                                 UserRepository userRepository, ItemRepository itemRepository,
                                 DailyEntryRepository dailyEntryRepository, PurchaseRepository purchaseRepository,
                                 RecommendationRepository recommendationRepository, AuditLogService auditLogService) {
        this.organizationRepository = organizationRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.dailyEntryRepository = dailyEntryRepository;
        this.purchaseRepository = purchaseRepository;
        this.recommendationRepository = recommendationRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/data-export")
    public TenantDataExportDto exportMyOrganizationData(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (!"OWNER".equals(principal.role())) {
            throw new AccessDeniedException("تصدير كامل البيانات متاح لصاحب المؤسسة فقط");
        }
        Long organizationId = principal.organizationId();
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NoSuchElementException("المؤسسة غير موجودة"));

        List<Branch> branches = branchRepository.findByOrganizationId(organizationId);
        List<Long> branchIds = branches.stream().map(Branch::getId).toList();
        List<User> users = userRepository.findByOrganizationId(organizationId);

        var branchExports = branches.stream()
                .map(b -> new TenantDataExportDto.BranchExport(b.getId(), b.getName(), b.getCity(), b.isActive()))
                .toList();

        var userExports = users.stream()
                .map(u -> new TenantDataExportDto.UserExport(u.getId(), u.getName(), u.getEmail(),
                        u.getRole().name(), u.getBranch() != null ? u.getBranch().getId() : null, u.isActive()))
                .toList();

        var itemExports = branchIds.isEmpty() ? List.<TenantDataExportDto.ItemExport>of()
                : itemRepository.findByBranchIdIn(branchIds).stream()
                .map(i -> new TenantDataExportDto.ItemExport(i.getId(), i.getBranch().getId(), i.getName(),
                        i.getSubCategory(), i.getCostPrice(), i.getSalePrice(), i.getQuantity(),
                        i.getLastSaleDate(), i.getExpiryDate(),
                        i.getMovementStatus() != null ? i.getMovementStatus().name() : null))
                .toList();

        var dailyEntryExports = branchIds.isEmpty() ? List.<TenantDataExportDto.DailyEntryExport>of()
                : dailyEntryRepository.findByBranchIdInOrderByEntryDateAsc(branchIds).stream()
                .map(e -> new TenantDataExportDto.DailyEntryExport(e.getId(), e.getBranch().getId(), e.getEntryDate(),
                        e.getTotalSales(), e.getTotalCogs(), e.getTotalProfit(), e.getAvailableLiquidity(),
                        e.getReceivables(), e.getPayables()))
                .toList();

        var purchaseExports = branchIds.isEmpty() ? List.<TenantDataExportDto.PurchaseExport>of()
                : purchaseRepository.findByBranchIdInOrderByPurchaseDateDesc(branchIds).stream()
                .map(p -> new TenantDataExportDto.PurchaseExport(p.getId(), p.getBranch().getId(),
                        p.getItem() != null ? p.getItem().getName() : null, p.getSupplierName(), p.getQuantity(),
                        p.getCostPrice(), p.getPurchaseDate()))
                .toList();

        var recommendationExports = branchIds.isEmpty() ? List.<TenantDataExportDto.RecommendationExport>of()
                : recommendationRepository.findByBranchIdInOrderByExpectedValueDesc(branchIds).stream()
                .map(r -> new TenantDataExportDto.RecommendationExport(r.getId(), r.getBranch().getId(),
                        r.getType().name(), r.getPriority().name(), r.getTitle(), r.getExpectedValue(),
                        r.getStatus() != null ? r.getStatus().name() : null, r.getCreatedAt(), r.getResolvedAt()))
                .toList();

        auditLogService.record(organizationId, principal.email(), "EXPORT_ALL_DATA", "Organization",
                String.valueOf(organizationId), "branches=" + branches.size() + ", items=" + itemExports.size());

        return new TenantDataExportDto(org.getId(), org.getName(), org.getCategory().name(), LocalDateTime.now(),
                branchExports, userExports, itemExports, dailyEntryExports, purchaseExports, recommendationExports);
    }
}
