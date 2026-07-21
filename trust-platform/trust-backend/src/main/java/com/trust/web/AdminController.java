package com.trust.web;

import com.trust.domain.Branch;
import com.trust.domain.Category;
import com.trust.domain.DailyEntry;
import com.trust.domain.Decision;
import com.trust.domain.Item;
import com.trust.domain.Organization;
import com.trust.domain.User;
import com.trust.repository.BranchRepository;
import com.trust.repository.DailyEntryRepository;
import com.trust.repository.DecisionRepository;
import com.trust.repository.ItemRepository;
import com.trust.repository.OrganizationRepository;
import com.trust.repository.UserRepository;
import com.trust.service.AuditLogService;
import com.trust.service.HealthScoreService;
import com.trust.web.dto.AdminCityBreakdownDto;
import com.trust.web.dto.AdminHealthDistributionDto;
import com.trust.web.dto.AdminOrganizationDto;
import com.trust.web.dto.AdminOverviewDto;
import com.trust.web.dto.AdminPlatformTrendPointDto;
import com.trust.web.dto.AdminRiskOpportunityDto;
import com.trust.web.dto.AdminStagnantItemDto;
import com.trust.web.dto.CreateOrganizationRequest;
import com.trust.web.dto.CreateOrganizationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.trust.config.AuthenticatedUser;

/**
 * لوحة تحكم الأدمن - القسم 8 من خطة MVP: نظرة عامة على المنصة + تجميع المخزون الراكد (عرض فقط).
 * كل نقطة تجلب المؤسسات/الفروع/الأصناف بطلبات مجمّعة (IN queries) بدل الاستعلام داخل حلقة
 * لكل مؤسسة/فرع على حدة - ضروري ليبقى الأداء ثابتًا مع نمو عدد المؤسسات على المنصة.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final ItemRepository itemRepository;
    private final DailyEntryRepository dailyEntryRepository;
    private final UserRepository userRepository;
    private final DecisionRepository decisionRepository;
    private final HealthScoreService healthScoreService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public AdminController(OrganizationRepository organizationRepository, BranchRepository branchRepository,
                            ItemRepository itemRepository, DailyEntryRepository dailyEntryRepository,
                            UserRepository userRepository, DecisionRepository decisionRepository,
                            HealthScoreService healthScoreService,
                            PasswordEncoder passwordEncoder, AuditLogService auditLogService) {
        this.organizationRepository = organizationRepository;
        this.branchRepository = branchRepository;
        this.itemRepository = itemRepository;
        this.dailyEntryRepository = dailyEntryRepository;
        this.userRepository = userRepository;
        this.decisionRepository = decisionRepository;
        this.healthScoreService = healthScoreService;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    /** يُنشئ مؤسسة جديدة + فرعها الأول + مستخدم OWNER بكلمة مرور مؤقتة تُعرض مرة واحدة (لا يوجد بريد فعلي بعد) */
    @PostMapping("/organizations")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateOrganizationResponse createOrganization(@Valid @RequestBody CreateOrganizationRequest request,
                                                           @AuthenticationPrincipal AuthenticatedUser principal) {
        if (userRepository.findByEmail(request.ownerEmail()).isPresent()) {
            throw new IllegalStateException("البريد الإلكتروني مستخدم مسبقًا");
        }
        Category category;
        try {
            category = Category.valueOf(request.category());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("تصنيف غير صالح");
        }

        Organization org = new Organization();
        org.setName(request.organizationName());
        org.setCategory(category);
        org = organizationRepository.save(org);

        Branch branch = new Branch();
        branch.setOrganization(org);
        branch.setName(request.branchName());
        branch.setCity(request.branchCity());
        branch = branchRepository.save(branch);

        String temporaryPassword = generatePassword();
        User owner = new User();
        owner.setOrganization(org);
        owner.setBranch(branch);
        owner.setName(request.ownerName());
        owner.setEmail(request.ownerEmail());
        owner.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        owner.setRole(User.Role.OWNER);
        owner.setActive(true);
        userRepository.save(owner);

        auditLogService.record(org.getId(), principal.email(), "CREATE_ORGANIZATION", "Organization", org.getId().toString(),
                "name=" + org.getName() + ", ownerEmail=" + owner.getEmail());

        return new CreateOrganizationResponse(org.getId(), org.getName(), branch.getId(), owner.getEmail(), temporaryPassword);
    }

    private static String generatePassword() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    @GetMapping("/organizations")
    public List<AdminOrganizationDto> listOrganizations() {
        return buildOrganizationList();
    }

    private List<AdminOrganizationDto> buildOrganizationList() {
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

        List<DailyEntry> recentEntries = branchIds.isEmpty() ? List.of()
                : dailyEntryRepository.findByBranchIdInAndEntryDateBetweenOrderByEntryDateAsc(
                        branchIds, LocalDate.now().minusDays(6), LocalDate.now());

        List<AdminPlatformTrendPointDto> salesTrend = recentEntries.stream()
                .collect(Collectors.groupingBy(DailyEntry::getEntryDate,
                        Collectors.summingDouble(DailyEntry::getTotalSales)))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new AdminPlatformTrendPointDto(e.getKey(), e.getValue()))
                .toList();

        double totalSalesToday = recentEntries.stream()
                .filter(e -> e.getEntryDate().equals(LocalDate.now()))
                .mapToDouble(DailyEntry::getTotalSales)
                .sum();

        List<AdminCityBreakdownDto> cityBreakdown = buildCityBreakdown(orgs, allBranches);
        AdminRiskOpportunityDto riskOpportunity = buildRiskOpportunity(branchIds);

        List<AdminOrganizationDto> allOrgs = buildOrganizationList();
        AdminHealthDistributionDto healthDistribution = buildHealthDistribution(allOrgs);
        List<AdminOrganizationDto> leaderboard = allOrgs.stream()
                .sorted(Comparator.comparingDouble(AdminOrganizationDto::avgHealthScore).reversed())
                .limit(5)
                .toList();

        return new AdminOverviewDto(orgs.size(), allBranches.size(),
                Math.round(avgHealth * 10) / 10.0, totalStagnantValue, totalSalesToday, byCategory,
                salesTrend, cityBreakdown, riskOpportunity, healthDistribution, leaderboard);
    }

    private AdminRiskOpportunityDto buildRiskOpportunity(List<Long> branchIds) {
        List<Decision> openDecisions = branchIds.isEmpty() ? List.of()
                : decisionRepository.findByBranchIdIn(branchIds).stream()
                        .filter(d -> d.getStatus() == Decision.Status.OPEN)
                        .toList();
        int risksCount = 0;
        double risksValue = 0;
        int opportunitiesCount = 0;
        double opportunitiesValue = 0;
        for (Decision d : openDecisions) {
            if (d.getCategory() == Decision.Category.RISK) {
                risksCount++;
                risksValue += d.getFinancialImpact();
            } else {
                opportunitiesCount++;
                opportunitiesValue += d.getFinancialImpact();
            }
        }
        return new AdminRiskOpportunityDto(risksCount, risksValue, opportunitiesCount, opportunitiesValue);
    }

    private AdminHealthDistributionDto buildHealthDistribution(List<AdminOrganizationDto> orgs) {
        int good = 0, medium = 0, poor = 0;
        for (AdminOrganizationDto org : orgs) {
            if (org.avgHealthScore() >= 61) good++;
            else if (org.avgHealthScore() >= 41) medium++;
            else poor++;
        }
        return new AdminHealthDistributionDto(good, medium, poor);
    }

    private List<AdminCityBreakdownDto> buildCityBreakdown(List<Organization> orgs, List<Branch> allBranches) {
        Map<Long, Organization> orgById = orgs.stream().collect(Collectors.toMap(Organization::getId, o -> o));
        Map<String, List<Branch>> branchesByCity = allBranches.stream()
                .collect(Collectors.groupingBy(b -> b.getCity() == null || b.getCity().isBlank() ? "غير محدد" : b.getCity()));

        return branchesByCity.entrySet().stream()
                .map(e -> {
                    List<Branch> cityBranches = e.getValue();
                    long orgCount = cityBranches.stream()
                            .map(b -> orgById.get(b.getOrganization().getId()).getId())
                            .distinct().count();
                    double avgScore = avgHealthScore(cityBranches);
                    return new AdminCityBreakdownDto(e.getKey(), (int) orgCount, cityBranches.size(),
                            Math.round(avgScore * 10) / 10.0);
                })
                .sorted(Comparator.comparingInt(AdminCityBreakdownDto::branchCount).reversed())
                .toList();
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
