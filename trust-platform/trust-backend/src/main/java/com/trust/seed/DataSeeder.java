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
    private final PurchaseRepository purchaseRepository;
    private final MonthlyExpenseRepository monthlyExpenseRepository;
    private final WasteRecordRepository wasteRecordRepository;
    private final StockCountRepository stockCountRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(OrganizationRepository organizationRepository, BranchRepository branchRepository,
                       CategoryBenchmarkRepository benchmarkRepository, ItemRepository itemRepository,
                       DailyEntryRepository dailyEntryRepository, UserRepository userRepository,
                       SupplierRepository supplierRepository, PurchaseRepository purchaseRepository,
                       MonthlyExpenseRepository monthlyExpenseRepository,
                       WasteRecordRepository wasteRecordRepository,
                       StockCountRepository stockCountRepository,
                       PasswordEncoder passwordEncoder) {
        this.organizationRepository = organizationRepository;
        this.branchRepository = branchRepository;
        this.benchmarkRepository = benchmarkRepository;
        this.itemRepository = itemRepository;
        this.dailyEntryRepository = dailyEntryRepository;
        this.userRepository = userRepository;
        this.supplierRepository = supplierRepository;
        this.purchaseRepository = purchaseRepository;
        this.monthlyExpenseRepository = monthlyExpenseRepository;
        this.wasteRecordRepository = wasteRecordRepository;
        this.stockCountRepository = stockCountRepository;
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

        // حجم محل يطابق شخصية المنتج المستهدفة في نموذج مدير المنتج: ~3,500 شيكل يوميًا
        // (~105,000 شهريًا). البذرة السابقة كانت ~85,000 يوميًا - أي هايبرماركت لا بقالة،
        // وهو ما كان يثبّت كل مؤشرات BHI عند السقف ويجعل العرض غير مقنع.
        //
        // ستون يومًا لأن BHI يقارن آخر ثلاثين بالثلاثين التي قبلها؛ بأربعة عشر يومًا فقط
        // تبتلع النافذة الحالية كل البيانات ويبقى مؤشر نمو المبيعات بلا فترة سابقة.
        double marginRatio = 0.22; // هامش إجمالي واقعي لبقالة - كان 33.1%، أعلى من أي حد "ممتاز"
        LocalDate start = LocalDate.now().minusDays(59);
        for (int i = 0; i < 60; i++) {
            LocalDate date = start.plusDays(i);
            double trend = 3150 + i * 8;              // نمو تدريجي => نمو مبيعات ~7%
            double weekly = weekdayFactor(date);      // تباين أسبوعي حتى لا يبدو الخط مصطنعًا
            double sales = Math.round(trend * weekly);

            DailyEntry entry = new DailyEntry();
            entry.setBranch(branch);
            entry.setEntryDate(date);
            entry.setTotalSales(sales);
            double profit = sales * marginRatio;
            entry.setTotalCogs(sales - profit);
            entry.setTotalProfit(profit);
            entry.setAvailableLiquidity(28000 - (59 - i) * 40);
            entry.setReceivables(45000);
            entry.setPayables(115000);
            dailyEntryRepository.save(entry);
        }

        // أصناف تجريبية بحالات حركة مختلفة
        saveItem(branch, "عصير تفاح 1 لتر", 3.2, 5.5, 6000, LocalDate.now().minusDays(75), null);
        saveItem(branch, "جبنة بيضاء 1 كغ", 12.0, 16.0, 2800, LocalDate.now().minusDays(35), LocalDate.now().plusDays(20));
        saveItem(branch, "شوكولاتة داكنة", 4.0, 9.5, 700, LocalDate.now().minusDays(5), null);
        saveItem(branch, "دجاج مجمد 900غ", 18.0, 24.0, 2800, LocalDate.now().minusDays(10), LocalDate.now().plusDays(60));
        Item rice = saveItem(branch, "أرز بسمتي 5 كغ", 22.0, 27.0, 120, LocalDate.now().minusDays(3), null);
        Item oliveOil = saveItem(branch, "زيت زيتون 1 لتر", 30.0, 42.0, 60, LocalDate.now().minusDays(2), null);

        // حقوق الملكية تفتح مؤشر نسبة الدين إلى حقوق الملكية (115,000 / 210,000 = 0.55)
        org.setEquity(210_000.0);
        organizationRepository.save(org);

        Supplier mainSupplier = seedSuppliers(org, rice, oliveOil);
        seedBhiDataSources(branch, rice, oliveOil);
        seedSecondOrganization(mainSupplier);
        seedUsers(org, branch);
    }

    /**
     * موردون تجريبيون - يُربط بعضهم بأصناف سريعة الحركة حتى يُنتج محرك قرار الشراء
     * (PurchaseDecisionEngineService) توصية حقيقية مدعومة بمدة توريد فعلية عند أول تشغيل.
     */
    private Supplier seedSuppliers(Organization org, Item rice, Item oliveOil) {
        Supplier mainSupplier = new Supplier();
        mainSupplier.setOrganization(org);
        mainSupplier.setName("شركة الأمين للتوريدات");
        mainSupplier.setContactInfo("0599123456");
        mainSupplier.setEmail("supplier@trust.demo");
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

        // أمر شراء مفتوح (بانتظار التوريد) عبر بوابة المورد - إدخال مباشر مثل النمط اليدوي القديم
        Purchase openPurchase = new Purchase();
        openPurchase.setBranch(rice.getBranch());
        openPurchase.setItem(rice);
        openPurchase.setSupplier(mainSupplier);
        openPurchase.setSupplierName(mainSupplier.getName());
        openPurchase.setQuantity(200);
        openPurchase.setCostPrice(rice.getCostPrice());
        openPurchase.setPurchaseDate(LocalDate.now().minusDays(1));
        openPurchase.setStatus(Purchase.Status.SENT);
        // يبقى PENDING عمدًا حتى يكون في بوابة المورّد أمر واحد قابل للقبول أو الاعتذار
        openPurchase.setSupplierResponse(Purchase.SupplierResponse.PENDING);
        purchaseRepository.save(openPurchase);

        return mainSupplier;
    }

    /** مؤسسة ثانية بسيطة لغرض تجربة تجميع البيانات عبر المؤسسات في لوحة الأدمن وبوابة المورد */
    private void seedSecondOrganization(Supplier sharedSupplier) {
        Organization pharmacy = new Organization();
        pharmacy.setName("صيدلية الشفاء");
        pharmacy.setCategory(Category.PHARMACY);
        pharmacy = organizationRepository.save(pharmacy);

        Branch branch = new Branch();
        branch.setOrganization(pharmacy);
        branch.setName("الفرع الرئيسي - نابلس");
        branch.setCity("نابلس");
        branch = branchRepository.save(branch);

        // صيدلية بحجم واقعي، وبستين يومًا من الإدخالات مثل السوبرماركت. بإدخال واحد فقط
        // كان طول الفترة يساوي يومًا واحدًا، فيُضرَب دوران المخزون في 365 ويثبت المحور عند
        // 100 بينما السوبرماركت عند 82 - أي أن شاشة الأدمن المبنية للمقارنة كانت تناقض نفسها.
        double pharmacyMargin = 0.28; // هامش الصيدلية أعلى من البقالة بطبيعته
        LocalDate pharmacyStart = LocalDate.now().minusDays(59);
        for (int i = 0; i < 60; i++) {
            LocalDate date = pharmacyStart.plusDays(i);
            double sales = Math.round((2400 + i * 4) * weekdayFactor(date));
            DailyEntry e = new DailyEntry();
            e.setBranch(branch);
            e.setEntryDate(date);
            e.setTotalSales(sales);
            double profit = sales * pharmacyMargin;
            e.setTotalCogs(sales - profit);
            e.setTotalProfit(profit);
            e.setAvailableLiquidity(19000 - (59 - i) * 30);
            e.setReceivables(26000);
            e.setPayables(72000);
            dailyEntryRepository.save(e);
        }
        pharmacy.setEquity(150_000.0);
        organizationRepository.save(pharmacy);

        // مخزون بقيمة ~68,000 بالتكلفة => دوران ~9 مرات/سنة، مع إبقاء فيتامين سي منخفضًا
        // حتى يظل محرك قرار الشراء ينتج توصية حقيقية
        saveItem(branch, "باراسيتامول 500مغ", 2.5, 4.0, 9000, LocalDate.now().minusDays(70), LocalDate.now().plusDays(180));
        Item vitaminC = saveItem(branch, "فيتامين سي فوار", 6.0, 11.0, 60, LocalDate.now().minusDays(4), LocalDate.now().plusDays(300));
        saveItem(branch, "كريم مرطب", 9.0, 18.0, 2500, LocalDate.now().minusDays(90), null);

        // نفس مصادر BHI حتى لا تبقى الصيدلية مُقيَّمة على سبعة مؤشرات بينما السوبرماركت على ثلاثة عشر
        LocalDate thisMonth = LocalDate.now().withDayOfMonth(1);
        seedExpense(branch, thisMonth, ExpenseCategory.MANAGER_SALARY, 4500, 1);
        seedExpense(branch, thisMonth, ExpenseCategory.STAFF_SALARY, 2200, 2);
        seedExpense(branch, thisMonth, ExpenseCategory.RENT, 2600, 1);
        seedExpense(branch, thisMonth, ExpenseCategory.ELECTRICITY, 1400, 1);
        seedExpense(branch, thisMonth, ExpenseCategory.SUNDRIES, 300, 1);

        // نفس المورّد الحقيقي (بريد مطابق) يخدم هذه المؤسسة أيضًا - سجل Supplier منفصل خاص بهذه المؤسسة
        // (كل مؤسسة تحتفظ بتقييمها وشروطها الخاصة)، لكنه يرتبط بحساب بوابة المورد نفسه عبر البريد
        Supplier pharmacySupplier = new Supplier();
        pharmacySupplier.setOrganization(pharmacy);
        pharmacySupplier.setName(sharedSupplier.getName());
        pharmacySupplier.setContactInfo(sharedSupplier.getContactInfo());
        pharmacySupplier.setEmail(sharedSupplier.getEmail());
        pharmacySupplier.setLeadTimeDays(6);
        pharmacySupplier.setCreditTermsDays(30);
        pharmacySupplier.setRating(85);
        pharmacySupplier = supplierRepository.save(pharmacySupplier);

        vitaminC.setSupplier(pharmacySupplier);
        vitaminC.setSafetyStockDays(3);
        itemRepository.save(vitaminC);

        // أمر شراء مستلم سابقًا لنفس المورد من هذه المؤسسة - يثبت أن بوابة المورد تجمع بيانات
        // حقيقية عبر أكثر من مؤسسة مستأجرة، وليس فقط من المؤسسة التي أُنشئ فيها الحساب
        Purchase receivedPurchase = new Purchase();
        receivedPurchase.setBranch(branch);
        receivedPurchase.setItem(vitaminC);
        receivedPurchase.setSupplier(pharmacySupplier);
        receivedPurchase.setSupplierName(pharmacySupplier.getName());
        receivedPurchase.setQuantity(60);
        receivedPurchase.setCostPrice(vitaminC.getCostPrice());
        receivedPurchase.setPurchaseDate(LocalDate.now().minusDays(10));
        receivedPurchase.setStatus(Purchase.Status.RECEIVED);
        receivedPurchase.setReceivedQuantity(60.0);
        receivedPurchase.setReceivedDate(LocalDate.now().minusDays(5));
        receivedPurchase.setPaymentDueDate(LocalDate.now().minusDays(2));
        receivedPurchase.setPaidOnDate(LocalDate.now().minusDays(3));
        purchaseRepository.save(receivedPurchase);

        seedWaste(branch, vitaminC, 18, WasteRecord.Reason.EXPIRY, 5);
        seedCount(branch, vitaminC, 60, 58, 4);
        for (int i = 0; i < 5; i++) {
            Purchase p = new Purchase();
            p.setBranch(branch);
            p.setItem(vitaminC);
            p.setSupplier(pharmacySupplier);
            p.setSupplierName(pharmacySupplier.getName());
            p.setQuantity(15);
            p.setCostPrice(vitaminC.getCostPrice());
            int daysAgo = 22 - (i * 4);
            p.setPurchaseDate(LocalDate.now().minusDays(daysAgo));
            p.setStatus(Purchase.Status.RECEIVED);
            p.setPaymentDueDate(LocalDate.now().minusDays(Math.max(1, daysAgo - 3)));
            p.setPaidOnDate(LocalDate.now().minusDays(Math.max(0, daysAgo - (i == 4 ? 6 : 4))));
            purchaseRepository.save(p);
        }
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

        User supplierUser = new User();
        supplierUser.setName("شركة الأمين للتوريدات");
        supplierUser.setEmail("supplier@trust.demo");
        supplierUser.setPasswordHash(passwordEncoder.encode("supplier123"));
        supplierUser.setRole(User.Role.SUPPLIER);
        userRepository.save(supplierUser);
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

    /**
     * يبذر المصادر التي تفتح المؤشرات الستة المتبقية في BHI، فيصبح العرض التجريبي
     * محسوبًا على ثلاثة عشر مؤشرًا لا سبعة.
     *
     * الأرقام مختارة لتعطي قراءات واقعية متنوعة - لا كلها ممتازة ولا كلها ضعيفة -
     * لأن لوحة تُظهر 100 في كل شيء لا تُثبت أن المؤشر يعمل.
     */
    /** تباين أسبوعي بسيط - نهاية الأسبوع أعلى، كما في أي بقالة */
    private double weekdayFactor(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case THURSDAY -> 1.12;
            case FRIDAY -> 1.18;
            case SATURDAY -> 1.05;
            case SUNDAY -> 0.92;
            case MONDAY -> 0.95;
            default -> 1.0;
        };
    }

    private void seedBhiDataSources(Branch branch, Item rice, Item oliveOil) {
        LocalDate thisMonth = LocalDate.now().withDayOfMonth(1);

        // جدول المصاريف الشهرية من نموذج مدير المنتج حرفيًا - إجماليه 16,800
        seedExpense(branch, thisMonth, ExpenseCategory.MANAGER_SALARY, 5000, 1);
        seedExpense(branch, thisMonth, ExpenseCategory.STAFF_SALARY, 2000, 3);
        seedExpense(branch, thisMonth, ExpenseCategory.ELECTRICITY, 2500, 1);
        seedExpense(branch, thisMonth, ExpenseCategory.RENT, 3000, 1);
        seedExpense(branch, thisMonth, ExpenseCategory.TECH_SERVICES, 200, 1);
        seedExpense(branch, thisMonth, ExpenseCategory.SUNDRIES, 100, 1);
        // الشهر السابق أيضًا حتى لا تنهار النسبة إن امتدت الفترة عبر شهرين
        seedExpense(branch, thisMonth.minusMonths(1), ExpenseCategory.MANAGER_SALARY, 5000, 1);
        seedExpense(branch, thisMonth.minusMonths(1), ExpenseCategory.STAFF_SALARY, 2000, 3);
        seedExpense(branch, thisMonth.minusMonths(1), ExpenseCategory.ELECTRICITY, 2400, 1);
        seedExpense(branch, thisMonth.minusMonths(1), ExpenseCategory.RENT, 3000, 1);
        seedExpense(branch, thisMonth.minusMonths(1), ExpenseCategory.TECH_SERVICES, 200, 1);
        seedExpense(branch, thisMonth.minusMonths(1), ExpenseCategory.SUNDRIES, 200, 1);

        // توالف بقيمة ~2,300 على مخزون ~110,000 => نسبة هدر ~2.1%
        seedWaste(branch, rice, 60, WasteRecord.Reason.EXPIRY, 9);
        seedWaste(branch, oliveOil, 32, WasteRecord.Reason.DAMAGE, 4);

        // جرد بفروقات صغيرة => دقة ~91%
        seedCount(branch, rice, 120, 112, 6);
        seedCount(branch, oliveOil, 60, 57, 6);

        // انضباط سداد ~82%: تسع فواتير في موعدها من أصل إحدى عشرة مستحقة
        for (int i = 0; i < 11; i++) {
            boolean late = i >= 9;
            Purchase p = new Purchase();
            p.setBranch(branch);
            p.setItem(i % 2 == 0 ? rice : oliveOil);
            p.setSupplierName("شركة الأمين للتوريدات");
            p.setQuantity(20);
            p.setCostPrice(i % 2 == 0 ? rice.getCostPrice() : oliveOil.getCostPrice());
            // موزّعة على 25 يومًا حتى تلتقطها نافذة الأسبوع في لوحة القيادة ونافذة
            // الثلاثين يومًا في شاشة المؤشر معًا
            int daysAgo = 25 - (i * 2);
            p.setPurchaseDate(LocalDate.now().minusDays(daysAgo));
            p.setStatus(Purchase.Status.RECEIVED);
            p.setPaymentDueDate(LocalDate.now().minusDays(Math.max(1, daysAgo - 3)));
            p.setPaidOnDate(LocalDate.now().minusDays(Math.max(0, daysAgo - (late ? 6 : 4))));
            purchaseRepository.save(p);
        }
    }

    private void seedExpense(Branch branch, LocalDate month, ExpenseCategory category,
                             double unitAmount, int quantity) {
        MonthlyExpense e = new MonthlyExpense();
        e.setBranch(branch);
        e.setExpenseMonth(month);
        e.setCategory(category);
        e.setUnitAmount(unitAmount);
        e.setQuantity(quantity);
        monthlyExpenseRepository.save(e);
    }

    private void seedWaste(Branch branch, Item item, double quantity,
                           WasteRecord.Reason reason, int daysAgo) {
        WasteRecord w = new WasteRecord();
        w.setBranch(branch);
        w.setItem(item);
        w.setWasteDate(LocalDate.now().minusDays(daysAgo));
        w.setQuantity(quantity);
        w.setUnitCost(item.getCostPrice());
        w.setReason(reason);
        wasteRecordRepository.save(w);
    }

    private void seedCount(Branch branch, Item item, double expected, double counted, int daysAgo) {
        StockCount c = new StockCount();
        c.setBranch(branch);
        c.setItem(item);
        c.setCountDate(LocalDate.now().minusDays(daysAgo));
        c.setExpectedQuantity(expected);
        c.setCountedQuantity(counted);
        stockCountRepository.save(c);
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
