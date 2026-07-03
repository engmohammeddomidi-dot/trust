package com.trust.seed;

import com.trust.domain.*;
import com.trust.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * يبذر بيانات تجريبية عند إقلاع التطبيق (بيئة H2 in-memory) — مؤسسة سوبرماركت
 * تجريبية بفرع واحد، أرقامها قريبة من التصميم الأصلي المرجعي للوحة القيادة،
 * بالإضافة لمؤسسة ثانية بسيطة ومستخدمين لتجربة تسجيل الدخول ولوحة الأدمن.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final CategoryBenchmarkRepository benchmarkRepository;
    private final ItemRepository itemRepository;
    private final DailyEntryRepository dailyEntryRepository;
    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(OrganizationRepository organizationRepository, BranchRepository branchRepository,
                       CategoryBenchmarkRepository benchmarkRepository, ItemRepository itemRepository,
                       DailyEntryRepository dailyEntryRepository, UserRepository userRepository,
                       SupplierRepository supplierRepository, PasswordEncoder passwordEncoder) {
        this.organizationRepository = organizationRepository;
        this.branchRepository = branchRepository;
        this.benchmarkRepository = benchmarkRepository;
        this.itemRepository = itemRepository;
        this.dailyEntryRepository = dailyEntryRepository;
        this.userRepository = userRepository;
        this.supplierRepository = supplierRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // على قاعدة بيانات دائمة (Postgres) تُعاد هذه الدالة عند كل إعادة تشغيل - البذر
        // مرة واحدة فقط عند أول إقلاع لتفادي تعارض المفاتيح الفريدة (مثل بريد المستخدم)
        if (organizationRepository.count() > 0) {
            log.info("Skipping demo data seeding - organizations already exist ({} found)", organizationRepository.count());
            return;
        }

        seedBenchmarks();

        Organization org = new Organization();
        org.setName("سوبرماركت النجمة");
        org.setCategory(Category.SUPERMARKET);
        org = organizationRepository.save(org);

        Branch branch = new Branch();
        branch.setOrganization(org);
        branch.setName("الفرع الرئيسي - رام الله");
        branch.setCity("رام الله");
        branch = branchRepository.save(branch);

        // سجل مبيعات آخر 7 أيام (مطابق تقريبًا لأرقام التصميم المرجعي)
        double[] sales = {68000, 74500, 89200, 94800, 90600, 95300, 86430};
        double marginRatio = 0.331; // هامش ربح ~33.1%
        LocalDate start = LocalDate.now().minusDays(6);
        for (int i = 0; i < sales.length; i++) {
            DailyEntry entry = new DailyEntry();
            entry.setBranch(branch);
            entry.setEntryDate(start.plusDays(i));
            entry.setTotalSales(sales[i]);
            double profit = sales[i] * marginRatio;
            entry.setTotalCogs(sales[i] - profit);
            entry.setTotalProfit(profit);
            entry.setAvailableLiquidity(415680 - (sales.length - 1 - i) * 3200);
            entry.setReceivables(312400);
            entry.setPayables(1019080);
            dailyEntryRepository.save(entry);
        }

        // أصناف تجريبية بحالات حركة مختلفة
        saveItem(branch, "عصير تفاح 1 لتر", 3.2, 5.5, 2850, LocalDate.now().minusDays(75), null);
        saveItem(branch, "جبنة بيضاء 1 كغ", 12.0, 16.0, 1240, LocalDate.now().minusDays(35), LocalDate.now().plusDays(20));
        saveItem(branch, "شوكولاتة داكنة", 4.0, 9.5, 320, LocalDate.now().minusDays(5), null);
        saveItem(branch, "دجاج مجمد 900غ", 18.0, 24.0, 860, LocalDate.now().minusDays(10), LocalDate.now().plusDays(60));
        Item rice = saveItem(branch, "أرز بسمتي 5 كغ", 22.0, 27.0, 500, LocalDate.now().minusDays(3), null);
        Item oliveOil = saveItem(branch, "زيت زيتون 1 لتر", 30.0, 42.0, 150, LocalDate.now().minusDays(2), null);

        seedSuppliers(org, rice, oliveOil);
        seedSecondOrganization();
        seedUsers(org, branch);
    }

    /**
     * موردون تجريبيون - يُربط بعضهم بأصناف سريعة الحركة حتى يُنتج محرك قرار الشراء
     * (PurchaseDecisionEngineService) توصية حقيقية مدعومة بمدة توريد فعلية عند أول تشغيل.
     */
    private void seedSuppliers(Organization org, Item rice, Item oliveOil) {
        Supplier mainSupplier = new Supplier();
        mainSupplier.setOrganization(org);
        mainSupplier.setName("شركة الأمين للتوريدات");
        mainSupplier.setContactInfo("0599123456");
        mainSupplier.setLeadTimeDays(5);
        mainSupplier.setCreditTermsDays(30);
        mainSupplier.setRating(92);
        mainSupplier = supplierRepository.save(mainSupplier);

        Supplier altSupplier = new Supplier();
        altSupplier.setOrganization(org);
        altSupplier.setName("مؤسسة النور التجارية");
        altSupplier.setContactInfo("0598765432");
        altSupplier.setLeadTimeDays(3);
        altSupplier.setCreditTermsDays(15);
        altSupplier.setRating(78);
        supplierRepository.save(altSupplier);

        rice.setSupplier(mainSupplier);
        rice.setSafetyStockDays(4);
        itemRepository.save(rice);

        oliveOil.setSupplier(mainSupplier);
        oliveOil.setSafetyStockDays(3);
        itemRepository.save(oliveOil);
    }

    /** مؤسسة ثانية بسيطة لغرض تجربة تجميع البيانات عبر المؤسسات في لوحة الأدمن */
    private void seedSecondOrganization() {
        Organization pharmacy = new Organization();
        pharmacy.setName("صيدلية الشفاء");
        pharmacy.setCategory(Category.PHARMACY);
        pharmacy = organizationRepository.save(pharmacy);

        Branch branch = new Branch();
        branch.setOrganization(pharmacy);
        branch.setName("الفرع الرئيسي - نابلس");
        branch.setCity("نابلس");
        branch = branchRepository.save(branch);

        DailyEntry entry = new DailyEntry();
        entry.setBranch(branch);
        entry.setEntryDate(LocalDate.now());
        entry.setTotalSales(22000);
        entry.setTotalCogs(15400);
        entry.setTotalProfit(6600);
        entry.setAvailableLiquidity(58000);
        entry.setReceivables(12000);
        entry.setPayables(41000);
        dailyEntryRepository.save(entry);

        saveItem(branch, "باراسيتامول 500مغ", 2.5, 4.0, 900, LocalDate.now().minusDays(70), LocalDate.now().plusDays(180));
        saveItem(branch, "فيتامين سي فوار", 6.0, 11.0, 60, LocalDate.now().minusDays(4), LocalDate.now().plusDays(300));
        saveItem(branch, "كريم مرطب", 9.0, 18.0, 210, LocalDate.now().minusDays(90), null);
    }

    private void seedUsers(Organization org, Branch branch) {
        User owner = new User();
        owner.setOrganization(org);
        owner.setBranch(branch);
        owner.setName("سامر خالد");
        owner.setEmail("owner@trust.demo");
        owner.setPasswordHash(passwordEncoder.encode("password123"));
        owner.setRole(User.Role.OWNER);
        userRepository.save(owner);

        User admin = new User();
        admin.setName("مدير المنصة");
        admin.setEmail("admin@trust.demo");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRole(User.Role.PLATFORM_ADMIN);
        userRepository.save(admin);
    }

    private Item saveItem(Branch branch, String name, double cost, double price, double qty,
                           LocalDate lastSale, LocalDate expiry) {
        Item item = new Item();
        item.setBranch(branch);
        item.setName(name);
        item.setCostPrice(cost);
        item.setSalePrice(price);
        item.setQuantity(qty);
        item.setLastSaleDate(lastSale);
        item.setExpiryDate(expiry);

        long daysSince = lastSale == null ? 999 : java.time.temporal.ChronoUnit.DAYS.between(lastSale, LocalDate.now());
        Item.MovementStatus status;
        if (daysSince > 60) status = Item.MovementStatus.STAGNANT;
        else if (daysSince > 30) status = Item.MovementStatus.SLOW;
        else if (daysSince > 14) status = Item.MovementStatus.MEDIUM;
        else status = Item.MovementStatus.FAST;
        item.setMovementStatus(status);

        return itemRepository.save(item);
    }

    private void seedBenchmarks() {
        for (Category category : Category.values()) {
            CategoryBenchmark bm = new CategoryBenchmark();
            bm.setCategory(category);
            bm.setTargetMarginPercent(switch (category) {
                case PHARMACY -> 28;
                case RESTAURANT -> 35;
                case RETAIL_CLOTHING -> 40;
                default -> 20;
            });
            bm.setLiquidityRatioMin(1.0);
            bm.setLiquidityRatioMax(2.0);
            bm.setInventoryCoverageMinMonths(1.0);
            bm.setInventoryCoverageMaxMonths(2.0);
            bm.setStagnationDaysThreshold(60);
            bm.setSlowMovingDaysThreshold(30);
            bm.setMediumMovingDaysThreshold(14);
            benchmarkRepository.save(bm);
        }
    }
}
