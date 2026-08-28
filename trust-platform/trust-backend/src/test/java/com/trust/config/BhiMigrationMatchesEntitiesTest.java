package com.trust.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * يربط كياني BHI بهجرة V8: كل عمود يولّده Hibernate من الكيان يجب أن يكون موجودًا في
 * ملف الهجرة.
 *
 * سبب وجوده: بروفايل التطوير يعتمد ddl-auto=update فيضيف الأعمدة الجديدة تلقائيًا،
 * بينما بروفايل الإنتاج يعتمد validate مع Flyway - أي أن إضافة حقل للكيان دون تحديث
 * الهجرة تمرّ بصمت محليًا وتُسقِط التطبيق عند النشر. هذا بالضبط ما حدث تاريخيًا مع
 * هجرات V2-V6 التي بقيت غير مُتحقَّق منها طويلًا.
 *
 * ملاحظة: لا يمكن تشغيل Flyway كاملًا على H2 لأن V7 تشير إلى اسم قيد يولّده Postgres
 * تحديدًا (app_users_role_check)، لذا نقارن هنا بنص الهجرة مباشرةً بدل تطبيقها.
 */
@SpringBootTest
class BhiMigrationMatchesEntitiesTest {

    /** كل جدول جديد أُضيف لأجل BHI، مقرونًا بالهجرة التي أنشأته */
    private static final Map<String, String> TABLE_TO_MIGRATION = Map.of(
            "bhi_thresholds", "db/migration/V8__add_bhi_thresholds_and_axis_weights.sql",
            "bhi_axis_weights", "db/migration/V8__add_bhi_thresholds_and_axis_weights.sql",
            "monthly_expenses", "db/migration/V9__add_monthly_expenses.sql",
            "waste_records", "db/migration/V10__add_waste_stock_counts_payment_and_equity.sql",
            "stock_counts", "db/migration/V10__add_waste_stock_counts_payment_and_equity.sql");

    /** أعمدة أُضيفت إلى جداول قائمة - تُفحص بالاسم لأن الجدول نفسه أقدم من الهجرة */
    private static final Map<String, String> ADDED_COLUMNS = Map.of(
            "payment_due_date", "db/migration/V10__add_waste_stock_counts_payment_and_equity.sql",
            "paid_on_date", "db/migration/V10__add_waste_stock_counts_payment_and_equity.sql",
            "equity", "db/migration/V10__add_waste_stock_counts_payment_and_equity.sql",
            "supplier_response", "db/migration/V11__add_supplier_response_to_purchases.sql",
            "supplier_responded_at", "db/migration/V11__add_supplier_response_to_purchases.sql",
            "supplier_promised_date", "db/migration/V11__add_supplier_response_to_purchases.sql",
            "supplier_rejection_reason", "db/migration/V11__add_supplier_response_to_purchases.sql");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private List<String> columnsOf(String table) {
        return jdbcTemplate.queryForList(
                "select column_name from information_schema.columns where upper(table_name) = ?",
                String.class, table.toUpperCase(Locale.ROOT));
    }

    private String scriptOf(String migration) throws Exception {
        try (var in = new ClassPathResource(migration).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        }
    }

    @Test
    void everyColumnHibernateDerivesFromTheNewTables_existsInItsMigration() throws Exception {
        for (Map.Entry<String, String> entry : TABLE_TO_MIGRATION.entrySet()) {
            String table = entry.getKey();
            String script = scriptOf(entry.getValue());

            List<String> columns = columnsOf(table);
            assertThat(columns).as("Hibernate should map the %s table", table).isNotEmpty();

            for (String column : columns) {
                assertThat(script)
                        .as("column %s.%s is missing from %s", table, column, entry.getValue())
                        .contains(column.toLowerCase(Locale.ROOT));
            }
        }
    }

    @Test
    void columnsAddedToPreexistingTables_areDeclaredInTheirMigration() throws Exception {
        for (Map.Entry<String, String> entry : ADDED_COLUMNS.entrySet()) {
            assertThat(scriptOf(entry.getValue()))
                    .as("column %s is missing from %s", entry.getKey(), entry.getValue())
                    .contains(entry.getKey());
        }
    }

    @Test
    void migrationsDeclareTheUniquenessThatLookupsRelyOn() throws Exception {
        String v8 = scriptOf("db/migration/V8__add_bhi_thresholds_and_axis_weights.sql");
        // البحث عن التجاوزات يفترض صفًا واحدًا على الأكثر لكل (فئة، مؤشر) و(فئة، محور)
        assertThat(v8).contains("unique (category, code)");
        assertThat(v8).contains("unique (category, axis)");

        // إدخال المصاريف يعتمد upsert على (فرع، شهر، بند)
        assertThat(scriptOf("db/migration/V9__add_monthly_expenses.sql"))
                .contains("unique (branch_id, expense_month, category)");
    }
}
