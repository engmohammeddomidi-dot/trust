package com.trust.service;

import com.trust.domain.Branch;
import com.trust.domain.CategoryBenchmark;
import com.trust.domain.Item;
import com.trust.repository.BranchRepository;
import com.trust.repository.CategoryBenchmarkRepository;
import com.trust.repository.ItemRepository;
import com.trust.web.dto.ItemCreateRequest;
import com.trust.web.dto.ItemDto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final BranchRepository branchRepository;
    private final CategoryBenchmarkRepository benchmarkRepository;

    public ItemService(ItemRepository itemRepository, BranchRepository branchRepository,
                        CategoryBenchmarkRepository benchmarkRepository) {
        this.itemRepository = itemRepository;
        this.branchRepository = branchRepository;
        this.benchmarkRepository = benchmarkRepository;
    }

    public ItemDto create(ItemCreateRequest req) {
        Branch branch = branchRepository.findById(req.branchId())
                .orElseThrow(() -> new IllegalArgumentException("الفرع غير موجود"));

        Item item = new Item();
        item.setBranch(branch);
        item.setName(req.name());
        item.setSubCategory(req.subCategory());
        item.setCostPrice(req.costPrice());
        item.setSalePrice(req.salePrice());
        item.setQuantity(req.quantity());
        item.setLastSaleDate(req.lastSaleDate() != null ? req.lastSaleDate() : LocalDate.now());
        item.setExpiryDate(req.expiryDate());
        item.setMovementStatus(classifyMovement(item, branch));

        Item saved = itemRepository.save(item);
        return toDto(saved);
    }

    public List<ItemDto> listByBranch(Long branchId) {
        return itemRepository.findByBranchId(branchId).stream().map(this::toDto).toList();
    }

    public List<ItemDto> listNeedingAttention(Long branchId) {
        return itemRepository.findByBranchId(branchId).stream()
                .filter(i -> i.getMovementStatus() == Item.MovementStatus.STAGNANT
                        || i.getMovementStatus() == Item.MovementStatus.SLOW
                        || (i.getExpiryDate() != null && !i.getExpiryDate().isBefore(LocalDate.now())
                            && ChronoUnit.DAYS.between(LocalDate.now(), i.getExpiryDate()) <= 30))
                .sorted((a, b) -> Double.compare(b.getInventoryValue(), a.getInventoryValue()))
                .map(this::toDto)
                .toList();
    }

    /** تصنيف حركة الصنف بناءً على تاريخ آخر بيع - القسم 5 من خطة MVP */
    public Item.MovementStatus classifyMovement(Item item, Branch branch) {
        CategoryBenchmark bm = benchmarkRepository.findById(branch.getOrganization().getCategory()).orElse(null);
        int stagnantDays = bm != null ? bm.getStagnationDaysThreshold() : 60;
        int slowDays = bm != null ? bm.getSlowMovingDaysThreshold() : 30;
        int mediumDays = bm != null ? bm.getMediumMovingDaysThreshold() : 14;

        if (item.getLastSaleDate() == null) return Item.MovementStatus.STAGNANT;
        long daysSinceLastSale = ChronoUnit.DAYS.between(item.getLastSaleDate(), LocalDate.now());

        if (daysSinceLastSale > stagnantDays) return Item.MovementStatus.STAGNANT;
        if (daysSinceLastSale > slowDays) return Item.MovementStatus.SLOW;
        if (daysSinceLastSale > mediumDays) return Item.MovementStatus.MEDIUM;
        return Item.MovementStatus.FAST;
    }

    private ItemDto toDto(Item i) {
        return new ItemDto(i.getId(), i.getName(), i.getSubCategory(), i.getCostPrice(), i.getSalePrice(),
                Math.round(i.getMarginPercent() * 10) / 10.0, i.getQuantity(), i.getInventoryValue(),
                i.getLastSaleDate(), i.getExpiryDate(), i.getMovementStatus().name());
    }
}
