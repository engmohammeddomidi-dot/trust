package com.trust.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** تصدير كامل لبيانات مؤسسة واحدة - يفي بحق تصدير البيانات المذكور في شروط الاستخدام */
public record TenantDataExportDto(
        Long organizationId,
        String organizationName,
        String category,
        LocalDateTime exportedAt,
        List<BranchExport> branches,
        List<UserExport> users,
        List<ItemExport> items,
        List<DailyEntryExport> dailyEntries,
        List<PurchaseExport> purchases,
        List<RecommendationExport> recommendations
) {
    public record BranchExport(Long id, String name, String city, boolean active) {}

    public record UserExport(Long id, String name, String email, String role, Long branchId, boolean active) {}

    public record ItemExport(Long id, Long branchId, String name, String subCategory, double costPrice,
                              double salePrice, double quantity, LocalDate lastSaleDate, LocalDate expiryDate,
                              String movementStatus) {}

    public record DailyEntryExport(Long id, Long branchId, LocalDate entryDate, double totalSales, double totalCogs,
                                    double totalProfit, double availableLiquidity, double receivables, double payables) {}

    public record PurchaseExport(Long id, Long branchId, String itemName, String supplierName, double quantity,
                                  double costPrice, LocalDate purchaseDate) {}

    public record RecommendationExport(Long id, Long branchId, String type, String priority, String title,
                                        double expectedValue, String status, LocalDateTime createdAt, LocalDateTime resolvedAt) {}
}
