package com.trust.service;

import com.trust.domain.Branch;
import com.trust.domain.DailyEntry;
import com.trust.repository.BranchRepository;
import com.trust.repository.DailyEntryRepository;
import com.trust.web.dto.DailyEntryRequest;
import org.springframework.stereotype.Service;

@Service
public class DailyEntryService {

    private final DailyEntryRepository dailyEntryRepository;
    private final BranchRepository branchRepository;

    public DailyEntryService(DailyEntryRepository dailyEntryRepository, BranchRepository branchRepository) {
        this.dailyEntryRepository = dailyEntryRepository;
        this.branchRepository = branchRepository;
    }

    public DailyEntry upsert(DailyEntryRequest req) {
        Branch branch = branchRepository.findById(req.branchId())
                .orElseThrow(() -> new IllegalArgumentException("الفرع غير موجود"));

        DailyEntry entry = dailyEntryRepository.findByBranchIdAndEntryDate(req.branchId(), req.entryDate())
                .orElseGet(DailyEntry::new);

        entry.setBranch(branch);
        entry.setEntryDate(req.entryDate());
        entry.setTotalSales(req.totalSales());
        entry.setTotalCogs(req.totalCogs());
        entry.setTotalProfit(req.totalProfit() != null ? req.totalProfit() : req.totalSales() - req.totalCogs());
        entry.setAvailableLiquidity(req.availableLiquidity());
        entry.setReceivables(req.receivables());
        entry.setPayables(req.payables());

        return dailyEntryRepository.save(entry);
    }
}
