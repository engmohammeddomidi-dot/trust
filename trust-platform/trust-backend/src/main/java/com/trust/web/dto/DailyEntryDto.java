package com.trust.web.dto;

import com.trust.domain.DailyEntry;

import java.time.LocalDate;

public record DailyEntryDto(
        Long id,
        Long branchId,
        LocalDate entryDate,
        double totalSales,
        double totalCogs,
        double totalProfit,
        double marginPercent,
        double availableLiquidity,
        double receivables,
        double payables
) {
    public static DailyEntryDto from(DailyEntry entry) {
        return new DailyEntryDto(
                entry.getId(),
                entry.getBranch().getId(),
                entry.getEntryDate(),
                entry.getTotalSales(),
                entry.getTotalCogs(),
                entry.getTotalProfit(),
                entry.getMarginPercent(),
                entry.getAvailableLiquidity(),
                entry.getReceivables(),
                entry.getPayables()
        );
    }
}
