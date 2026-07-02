package com.trust.web;

import com.trust.domain.Branch;
import com.trust.domain.DailyEntry;
import com.trust.domain.Item;
import com.trust.domain.Organization;
import com.trust.repository.BranchRepository;
import com.trust.repository.DailyEntryRepository;
import com.trust.repository.ItemRepository;
import com.trust.repository.OrganizationRepository;
import com.trust.service.HealthScoreService;
import com.trust.web.dto.AdminOrganizationDto;
import com.trust.web.dto.AdminOverviewDto;
import com.trust.web.dto.AdminStagnantItemDto;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * لوحة تحكم الأدمن - القسم 8 من خطة MVP: نظرة عامة على المنصة + تجميع المخزون الراكد (عرض فقط).
 * كل نقطة تجلب المؤسسات/الفروع/الأصناف بطلبات مجمّعة (IN queries) بدل الاستعلام داخل حلقة
 * لكل مؤسسة/فرع على حدة - ضروري ليبقى الأداء ثابتًا مع نمو عدد المؤسسات على المنصة.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final ItemRepository itemRepository;
    private final DailyEntryRepository dailyEntryRepository;
    private final HealthScoreService healthScoreService;

    public AdminController(OrganizationRepository organizationRepository, BranchRepository branchRepository,
                            ItemRepository itemRepository, DailyEntryRepository dailyEntryRepository,
                            HealthScoreService healthScoreService) {
        this.organizationRepository = organizationRepository;
        this.branchRepository = branchRepository;
        this.itemRepository = itemRepository;
        this.dailyEntryRepository = dailyEntryRepository;
        this.healthScoreService = healthScoreService;
    }

    @GetMapping("/organizations")
    public List<AdminOrganizationDto> listOrganizations() {
        List<Organization> orgs = organizationRepository.findAll();
        List<Branch> branches = branchRepository.findByOrganizationIdIn(orgs.stream().map(Organization::getId).toList());
        Map<Long, List<Branch>> branchesByOrg = branches.stream().collect(Collectors.groupingBy(b -> b.getOrganization().getId()));

        List<Long> branchIds = branches.stream().map(Branch::getId).toList();
        Map<Long, LocalDate> lastActivityByBranch = lastActivityByBranch(branchIds);

        return orgs.stream().map(org -> {
            List<Branch> orgBranches = branchesByOrg.getOrDefault(org.getId(), List.of());
            double avgScore = avgHealthScore(orgBranches);
            LocalDate lastActivity = orgBranches.stream()
                    .map(b -> lastActivityByBranch.get(b.getId()))
                    .filter(java.util.Objects::nonNull)
                    .max(LocalDate::compareTo)
                    .orElse(null);
            return new AdminOrganizationDto(org.getId(), org.getName(), org.getCategory().name(),
                    orgBranches.size(), Math.round(avgScore * 10) / 10.0, lastActivity);
        }).toList();
    }

    @GetMapping("/stagnant-items/aggregate")
    public List<AdminStagnantItemDto> stagnantItems() {
        List<Organization> orgs = organizationRepository.findAll();
        Map<Long, Organization> orgById = orgs.stream().collect(Collectors.toMap(Organization::getId, o -> o));
        List<Branch> branches = branchRepository.findByOrganizationIdIn(orgs.stream().map(Organization::getId).toList());
        List<Long> branchIds = branches.stream().map(Branch::getId).toList();
        List<Item> items = branchIds.isEmpty() ? List.of() : itemRepository.findByBranchIdIn(branchIds);

        List<AdminStagnantItemDto> results = new java.util.ArrayList<>();
        Map<Long, Branch> branchById = branches.stream().collect(Collectors.toMap(Branch::getId, b -> b));
        for (Item item : items) {
            if (item.getMovementStatus() != Item.MovementStatus.STAGNANT) continue;
            Branch branch = branchById.get(item.getBranch().getId());
            Organization org = orgById.get(branch.getOrganization().getId());
            results.add(new AdminStagnantItemDto(org.getName(), branch.getName(), item.getName(),
                    item.getQuantity(), item.getInventoryValue(), item.getLastSaleDate()));
        }
        results.sort(Comparator.comparingDouble(AdminStagnantItemDto::inventoryValue).reversed());
        return results;
    }

    @GetMapping("/overview")
    public AdminOverviewDto overview() {
        List<Organization> orgs = organizationRepository.findAll();
        List<Branch> allBranches = branchRepository.findByOrganizationIdIn(orgs.stream().map(Organization::getId).toList());
        List<Long> branchIds = allBranches.stream().map(Branch::getId).toList();
        List<Item> allItems = branchIds.isEmpty() ? List.of() : itemRepository.findByBranchIdIn(branchIds);

        double avgHealth = avgHealthScore(allBranches);

        double totalStagnantValue = allItems.stream()
                .filter(i -> i.getMovementStatus() == Item.MovementStatus.STAGNANT)
                .mapToDouble(Item::getInventoryValue)
                .sum();

        var byCategory = orgs.stream().collect(Collectors.groupingBy(
                o -> o.getCategory().name(), Collectors.counting()));

        return new AdminOverviewDto(orgs.size(), allBranches.size(),
                Math.round(avgHealth * 10) / 10.0, totalStagnantValue, byCategory);
    }

    private double avgHealthScore(List<Branch> branches) {
        if (branches.isEmpty()) return 0;
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(30);
        return branches.stream()
                .mapToDouble(b -> healthScoreService.calculate(b, from, to).totalScore())
                .average().orElse(0);
    }

    private Map<Long, LocalDate> lastActivityByBranch(List<Long> branchIds) {
        if (branchIds.isEmpty()) return Map.of();
        List<DailyEntry> entries = dailyEntryRepository.findByBranchIdInAndEntryDateBetweenOrderByEntryDateAsc(
                branchIds, LocalDate.now().minusYears(1), LocalDate.now());
        return entries.stream().collect(Collectors.toMap(
                e -> e.getBranch().getId(), DailyEntry::getEntryDate,
                (a, b) -> a.isAfter(b) ? a : b
        ));
    }
}
