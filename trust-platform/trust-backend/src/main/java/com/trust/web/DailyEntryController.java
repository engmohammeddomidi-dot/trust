package com.trust.web;

import com.trust.config.AuthenticatedUser;
import com.trust.config.TenantAccessGuard;
import com.trust.domain.DailyEntry;
import com.trust.repository.DailyEntryRepository;
import com.trust.service.DailyEntryService;
import com.trust.web.dto.DailyEntryDto;
import com.trust.web.dto.DailyEntryRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/entries/daily")
public class DailyEntryController {

    private final DailyEntryService dailyEntryService;
    private final DailyEntryRepository dailyEntryRepository;
    private final TenantAccessGuard accessGuard;

    public DailyEntryController(DailyEntryService dailyEntryService, DailyEntryRepository dailyEntryRepository,
                                 TenantAccessGuard accessGuard) {
        this.dailyEntryService = dailyEntryService;
        this.dailyEntryRepository = dailyEntryRepository;
        this.accessGuard = accessGuard;
    }

    @GetMapping
    public List<DailyEntryDto> list(@RequestParam Long branchId,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                     @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireBranch(principal, branchId);
        return dailyEntryRepository.findByBranchIdAndEntryDateBetweenOrderByEntryDateAsc(branchId, from, to)
                .stream().map(DailyEntryDto::from).toList();
    }

    @PostMapping
    public DailyEntryDto upsert(@Valid @RequestBody DailyEntryRequest request, @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireBranch(principal, request.branchId());
        DailyEntry entry = dailyEntryService.upsert(request);
        return DailyEntryDto.from(entry);
    }
}
