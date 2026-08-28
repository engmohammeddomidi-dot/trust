package com.trust.web;

import com.trust.config.AuthenticatedUser;
import com.trust.config.TenantAccessGuard;
import com.trust.domain.Branch;
import com.trust.domain.Item;
import com.trust.domain.StockCount;
import com.trust.domain.WasteRecord;
import com.trust.repository.ItemRepository;
import com.trust.repository.StockCountRepository;
import com.trust.repository.WasteRecordRepository;
import com.trust.web.dto.StockCountDto;
import com.trust.web.dto.StockCountRequest;
import com.trust.web.dto.WasteRecordDto;
import com.trust.web.dto.WasteRecordRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * التوالف والجرد الفعلي - مصدرا مؤشرَي نسبة الهدر ودقة الجرد في محور إدارة المخزون.
 *
 * كلاهما أداة تشغيلية قائمة بذاتها أيضًا: سجل التوالف يكشف أين يضيع المال، والجرد
 * يكشف الفروقات بين الدفتري والفعلي. تسجيل التالف يخصم الكمية من الصنف مباشرةً،
 * والجرد يصحّح الكمية الدفترية إلى المعدودة - وإلا بقيت الأرقام تنحرف بلا معالجة.
 */
@RestController
public class InventoryQualityController {

    private final WasteRecordRepository wasteRepository;
    private final StockCountRepository stockCountRepository;
    private final ItemRepository itemRepository;
    private final TenantAccessGuard accessGuard;

    public InventoryQualityController(WasteRecordRepository wasteRepository,
                                      StockCountRepository stockCountRepository,
                                      ItemRepository itemRepository,
                                      TenantAccessGuard accessGuard) {
        this.wasteRepository = wasteRepository;
        this.stockCountRepository = stockCountRepository;
        this.itemRepository = itemRepository;
        this.accessGuard = accessGuard;
    }

    // ---------------- التوالف ----------------

    @GetMapping("/api/waste")
    public List<WasteRecordDto> listWaste(@RequestParam Long branchId,
                                          @RequestParam(required = false) String from,
                                          @RequestParam(required = false) String to,
                                          @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireBranch(principal, branchId);
        LocalDate toDate = to != null ? LocalDate.parse(to) : LocalDate.now();
        LocalDate fromDate = from != null ? LocalDate.parse(from) : toDate.minusDays(30);
        return wasteRepository.findByBranchIdAndWasteDateBetween(branchId, fromDate, toDate)
                .stream().map(InventoryQualityController::toDto).toList();
    }

    @PostMapping("/api/waste")
    public WasteRecordDto recordWaste(@Valid @RequestBody WasteRecordRequest request,
                                      @AuthenticationPrincipal AuthenticatedUser principal) {
        Branch branch = accessGuard.requireBranch(principal, request.branchId());
        Item item = itemRepository.findById(request.itemId()).orElseThrow();
        if (!item.getBranch().getId().equals(branch.getId())) {
            throw new IllegalArgumentException("الصنف لا ينتمي لهذا الفرع");
        }

        WasteRecord record = new WasteRecord();
        record.setBranch(branch);
        record.setItem(item);
        record.setWasteDate(request.wasteDate() != null ? LocalDate.parse(request.wasteDate()) : LocalDate.now());
        record.setQuantity(request.quantity());
        // تكلفة الوحدة تُثبَّت لحظة التسجيل حتى لا يتغيّر التاريخ بتغيّر سعر الصنف لاحقًا
        record.setUnitCost(item.getCostPrice());
        record.setReason(WasteRecord.Reason.valueOf(request.reason()));
        record.setNote(request.note());

        // التالف يخرج من المخزون فعليًا - وإلا بقيت الكمية الدفترية أعلى من الواقع
        item.setQuantity(Math.max(0, item.getQuantity() - request.quantity()));
        itemRepository.save(item);

        return toDto(wasteRepository.save(record));
    }

    // ---------------- الجرد الفعلي ----------------

    @GetMapping("/api/stock-counts")
    public List<StockCountDto> listCounts(@RequestParam Long branchId,
                                          @RequestParam(required = false) String from,
                                          @RequestParam(required = false) String to,
                                          @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireBranch(principal, branchId);
        LocalDate toDate = to != null ? LocalDate.parse(to) : LocalDate.now();
        LocalDate fromDate = from != null ? LocalDate.parse(from) : toDate.minusDays(30);
        return stockCountRepository.findByBranchIdAndCountDateBetween(branchId, fromDate, toDate)
                .stream().map(InventoryQualityController::toDto).toList();
    }

    @PostMapping("/api/stock-counts")
    public StockCountDto recordCount(@Valid @RequestBody StockCountRequest request,
                                     @AuthenticationPrincipal AuthenticatedUser principal) {
        Branch branch = accessGuard.requireBranch(principal, request.branchId());
        Item item = itemRepository.findById(request.itemId()).orElseThrow();
        if (!item.getBranch().getId().equals(branch.getId())) {
            throw new IllegalArgumentException("الصنف لا ينتمي لهذا الفرع");
        }

        StockCount count = new StockCount();
        count.setBranch(branch);
        count.setItem(item);
        count.setCountDate(request.countDate() != null ? LocalDate.parse(request.countDate()) : LocalDate.now());
        // الكمية المتوقَّعة تُلتقط من الدفتر قبل التصحيح - هي أساس قياس الدقة
        count.setExpectedQuantity(item.getQuantity());
        count.setCountedQuantity(request.countedQuantity());
        count.setNote(request.note());

        // الجرد يصحّح الدفتر إلى الواقع، وإلا تكرّر الفارق نفسه في كل جرد لاحق
        item.setQuantity(request.countedQuantity());
        itemRepository.save(item);

        return toDto(stockCountRepository.save(count));
    }

    private static WasteRecordDto toDto(WasteRecord w) {
        return new WasteRecordDto(w.getId(), w.getItem().getId(), w.getItem().getName(),
                w.getWasteDate().toString(), w.getQuantity(), w.getUnitCost(), w.getTotalCost(),
                w.getReason().name(), w.getNote());
    }

    private static StockCountDto toDto(StockCount c) {
        return new StockCountDto(c.getId(), c.getItem().getId(), c.getItem().getName(),
                c.getCountDate().toString(), c.getExpectedQuantity(), c.getCountedQuantity(),
                c.getDiscrepancy(), c.getNote());
    }
}
