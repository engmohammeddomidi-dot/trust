package com.trust.web;

import com.trust.domain.Branch;
import com.trust.domain.Category;
import com.trust.domain.DailyEntry;
import com.trust.domain.Item;
import com.trust.domain.Organization;
import com.trust.domain.User;
import com.trust.repository.BranchRepository;
import com.trust.repository.DailyEntryRepository;
import com.trust.repository.ItemRepository;
import com.trust.repository.OrganizationRepository;
import com.trust.repository.UserRepository;
import com.trust.service.AuditLogService;
import com.trust.service.HealthScoreService;
import com.trust.web.dto.AdminOrganizationDto;
import com.trust.web.dto.AdminOverviewDto;
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
    private final HealthScoreService healthScoreService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public AdminController(OrganizationRepository organizationRepository, BranchRepository branchRepository,
                            ItemRepository itemRepository, DailyEntryRepository dailyEntryRepository,
                            UserRepository userRepository, HealthScoreService healthScoreService,
                            PasswordEncoder passwordEncoder, AuditLogService auditLogService) {
        this.organizationRepository = organizationRepository;
        this.branchRepository = branchRepository;
        this.itemRepository = itemRepository;
        this.dailyEntryRepository = dailyEntryRepository;
        this.userRepository = userRepository;
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
