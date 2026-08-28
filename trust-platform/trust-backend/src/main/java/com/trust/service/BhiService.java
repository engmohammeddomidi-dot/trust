package com.trust.service;

import com.trust.domain.*;
import com.trust.repository.*;
import com.trust.service.BhiMetricsCalculator.RawInputs;
import com.trust.web.dto.BhiResultDto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * مؤشر صحة الأعمال (BHI) وفق النموذج المرجعي المعتمد من مدير المنتج.
 *
 * يجمع الطبقات الثلاث: قراءة البيانات من المستودعات، ثم اشتقاق القيم الخام
 * (BhiMetricsCalculator)، ثم تحويلها إلى درجات وتجميعها (BhiScoringEngine).
 * الحدود والأوزان تُقرأ من جدولَي التجاوزات، وإلا فالقيم الافتراضية من النموذج المرجعي.
 */
@Service
public class BhiService {

    private final DailyEntryRepository dailyEntryRepository;
    private final ItemRepository itemRepository;
    private final BhiThresholdRepository thresholdRepository;
    private final BhiAxisWeightRepository axisWeightRepository;
    private final PurchaseRepository purchaseRepository;
    private final StockCountRepository stockCountRepository;
    private final WasteRecordRepository wasteRecordRepository;
    private final MonthlyExpenseService expenseService;
    private final BhiMetricsCalculator calculator;
    private final BhiScoringEngine engine;

    public BhiService(DailyEntryRepository dailyEntryRepository,
                      ItemRepository itemRepository,
                      BhiThresholdRepository thresholdRepository,
                      BhiAxisWeightRepository axisWeightRepository,
                      PurchaseRepository purchaseRepository,
                      StockCountRepository stockCountRepository,
                      WasteRecordRepository wasteRecordRepository,
                      MonthlyExpenseService expenseService,
                      BhiMetricsCalculator calculator,
                      BhiScoringEngine engine) {
        this.dailyEntryRepository = dailyEntryRepository;
        this.itemRepository = itemRepository;
        this.thresholdRepository = thresholdRepository;
        this.axisWeightRepository = axisWeightRepository;
        this.purchaseRepository = purchaseRepository;
        this.stockCountRepository = stockCountRepository;
        this.wasteRecordRepository = wasteRecordRepository;
        this.expenseService = expenseService;
        this.calculator = calculator;
        this.engine = engine;
    }

    public BhiResultDto calculate(Branch branch, LocalDate from, LocalDate to) {
        int periodDays = (int) Math.max(1, ChronoUnit.DAYS.between(from, to));

        List<DailyEntry> current = dailyEntryRepository
                .findByBranchIdAndEntryDateBetweenOrderByEntryDateAsc(branch.getId(), from, to);
        // فترة سابقة بنفس الطول تمامًا - النموذج المرجعي يقارن فترة بفترة، لا نصف فترة بنصفها
        List<DailyEntry> previous = dailyEntryRepository
                .findByBranchIdAndEntryDateBetweenOrderByEntryDateAsc(
                        branch.getId(), from.minusDays(periodDays), from.minusDays(1));
        List<Item> items = itemRepository.findByBranchId(branch.getId());

        RawInputs inputs = withExternalSources(
                assembleInputs(current, previous, items), branch, from, to);
        Category category = branch.getOrganization().getCategory();

        return engine.aggregate(indicatorInputs(inputs, category), axisWeights(category));
    }

    /** نتيجة مؤسسة متعددة الفروع - متوسط نتائج فروعها */
    public BhiResultDto averageAcross(List<Branch> branches, LocalDate from, LocalDate to) {
        return engine.average(branches.stream().map(b -> calculate(b, from, to)).toList());
    }

    /**
     * تجميع المدخلات الخام. التمييز الحاسم: المبيعات وتكلفة البضاعة تدفقات تُجمَع على
     * طول الفترة، أما السيولة والذمم فأرصدة لحظية تُؤخذ من آخر إدخال - جمعها عبر الأيام
     * يضخّمها ثلاثين ضعفًا بصمت.
     */
    RawInputs assembleInputs(List<DailyEntry> current, List<DailyEntry> previous, List<Item> items) {
        DailyEntry latest = current.stream()
                .max(Comparator.comparing(DailyEntry::getEntryDate))
                .orElse(null);

        return RawInputs.builder()
                // طول الفترة من عدد الأيام المُدخَلة فعليًا لا من طول التقويم: سبعة
                // إدخالات في نافذة ثلاثين يومًا لا تعني ثلاثة وعشرين يومًا بلا مبيعات
                .periodDays(Math.max(1, current.size()))
                .previousPeriodDays(Math.max(1, previous.size()))
                .currentPeriodSales(sum(current, DailyEntry::getTotalSales))
                .previousPeriodSales(sum(previous, DailyEntry::getTotalSales))
                .costOfGoodsSold(sum(current, DailyEntry::getTotalCogs))
                .inventoryValue(items.stream().mapToDouble(Item::getInventoryValue).sum())
                .availableLiquidity(latest == null ? 0 : latest.getAvailableLiquidity())
                .receivables(latest == null ? 0 : latest.getReceivables())
                .payables(latest == null ? 0 : latest.getPayables())
                // المصادر التالية غير موجودة في المنصة بعد - تبقى فارغة بصدق بدل تلفيقها
                .operatingExpenses(null)
                .wasteRatio(null)
                .stockAccuracy(null)
                .paymentEfficiency(null)
                .debtToEquity(null)
                .build();
    }

    /**
     * يضيف المؤشرات الآتية من جداول خارج الإدخال اليومي: المصاريف التشغيلية، التوالف،
     * الجرد الفعلي، انضباط السداد، وحقوق الملكية. كلٌّ منها يبقى فارغًا إن لم يُسجَّل
     * بعد - فالمحرك يعرض "غير متاح" ولا يخترع رقمًا.
     */
    private RawInputs withExternalSources(RawInputs base, Branch branch, LocalDate from, LocalDate to) {
        Long branchId = branch.getId();
        Double opex = expenseService.proratedForPeriod(branchId, from, to, base.periodDays());

        double wasteValue = wasteRecordRepository.findByBranchIdAndWasteDateBetween(branchId, from, to)
                .stream().mapToDouble(WasteRecord::getTotalCost).sum();
        // عدم وجود سجلات توالف غامض: قد يعني عدم وجود هدر، وقد يعني أن أحدًا لم يسجّله.
        // فنبقيه "غير متاح" - على عكس الجرد، حيث الجرد نفسه فعل صريح ونتيجته صفرًا تعني
        // أداءً ممتازًا لا بيانات ناقصة.
        Double waste = wasteValue > 0 ? wasteRatio(wasteValue, base.inventoryValue()) : null;

        Double accuracy = stockAccuracy(stockCountRepository.findByBranchIdAndCountDateBetween(branchId, from, to));

        Double punctuality = paymentEfficiency(
                purchaseRepository.findByBranchIdAndPurchaseDateBetween(branchId, from, to), to);

        // المؤسسة متاحة عبر الفرع الممرَّر - لا داعي لاستعلام إضافي، والدالة تُستدعى
        // مرة لكل مؤسسة ومدينة في لوحة الأدمن
        Double leverage = debtToEquity(base.payables(), branch.getOrganization().getEquity());

        return new RawInputs(base.periodDays(), base.previousPeriodDays(), base.currentPeriodSales(),
                base.previousPeriodSales(), base.costOfGoodsSold(), base.inventoryValue(),
                base.availableLiquidity(), base.receivables(), base.payables(),
                opex, waste, accuracy, punctuality, leverage);
    }

    // ---------------- المؤشرات التي كانت تنتظر مصادر بيانات ----------------

    /**
     * نسبة الفواتير المسدَّدة في موعدها. الفاتورة غير المسدَّدة التي تجاوزت استحقاقها
     * تُحتسب متأخرة - لا مجهولة، وإلا بدا المتعثّر منضبطًا لمجرد أنه لم يسدّد. أما التي
     * لم يحن أجلها بعد فتُستبعد لأن نتيجتها لم تُعرف.
     */
    Double paymentEfficiency(List<Purchase> purchases, LocalDate asOf) {
        List<Purchase> settled = purchases.stream()
                .filter(p -> p.getPaymentDueDate() != null)
                .filter(p -> p.getPaidOnDate() != null || !p.getPaymentDueDate().isAfter(asOf))
                .toList();
        if (settled.isEmpty()) return null;

        long onTime = settled.stream()
                .filter(p -> p.getPaidOnDate() != null && !p.getPaidOnDate().isAfter(p.getPaymentDueDate()))
                .count();
        return (double) onTime / settled.size();
    }

    /** الالتزامات على حقوق الملكية - فارغة إن لم تُسجَّل حقوق الملكية بعد */
    Double debtToEquity(double liabilities, Double equity) {
        if (equity == null || equity == 0) return null;
        return liabilities / equity;
    }

    /** قيمة التوالف على قيمة المخزون */
    Double wasteRatio(double wasteValue, double inventoryValue) {
        if (inventoryValue <= 0) return null;
        return wasteValue / inventoryValue;
    }

    /**
     * دقة الجرد = 1 ناقص نسبة الفارق المطلق. جردٌ بلا فروقات يعطي 100% - وهو أداء
     * ممتاز، لا بيانات ناقصة. عدم وجود جرد أصلًا هو ما يعطي "غير متاح".
     */
    Double stockAccuracy(List<StockCount> counts) {
        if (counts.isEmpty()) return null;
        double expected = counts.stream().mapToDouble(StockCount::getExpectedQuantity).sum();
        if (expected <= 0) return null;
        double discrepancy = counts.stream().mapToDouble(c -> Math.abs(c.getDiscrepancy())).sum();
        return Math.max(0.0, 1.0 - (discrepancy / expected));
    }

    private double sum(List<DailyEntry> entries, java.util.function.ToDoubleFunction<DailyEntry> f) {
        return entries.stream().mapToDouble(f).sum();
    }

    /** دمج القيم المحسوبة مع الحدود المعتمدة لفئة النشاط */
    private List<BhiScoringEngine.IndicatorInput> indicatorInputs(RawInputs raw, Category category) {
        Map<BhiIndicatorCode, Double> values = calculator.compute(raw);

        Map<BhiIndicatorCode, BhiThreshold> overrides = new EnumMap<>(BhiIndicatorCode.class);
        thresholdRepository.findByCategory(category).forEach(t -> overrides.put(t.getCode(), t));

        List<BhiScoringEngine.IndicatorInput> inputs = new ArrayList<>();
        for (BhiIndicatorCode code : BhiIndicatorCode.values()) {
            BhiThreshold o = overrides.get(code);
            inputs.add(new BhiScoringEngine.IndicatorInput(
                    code,
                    values.get(code),
                    o != null ? o.getWeakThreshold() : code.getDefaultWeak(),
                    o != null ? o.getMediumThreshold() : code.getDefaultMedium(),
                    o != null ? o.getExcellentThreshold() : code.getDefaultExcellent()));
        }
        return inputs;
    }

    private Map<BhiAxis, Double> axisWeights(Category category) {
        Map<BhiAxis, Double> weights = new EnumMap<>(BhiAxis.class);
        for (BhiAxis axis : BhiAxis.values()) {
            weights.put(axis, axis.getDefaultWeight());
        }
        axisWeightRepository.findByCategory(category)
                .forEach(w -> weights.put(w.getAxis(), w.getWeight()));
        return weights;
    }
}
