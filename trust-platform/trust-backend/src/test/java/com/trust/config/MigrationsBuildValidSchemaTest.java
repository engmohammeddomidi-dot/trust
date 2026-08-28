package com.trust.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * يبني المخطط من هجرات Flyway وحدها ثم يترك Hibernate يتحقق أنه يطابق كيانات JPA
 * (ddl-auto=validate) - وهو بالضبط ما يفعله الإنتاج عند الإقلاع.
 *
 * لماذا موقع هجرات مختلف: النسخة الأصلية من V7 تُسقط قيدًا باسم يولّده Postgres
 * تحديدًا (app_users_role_check)، ولا يحمل القيد ذلك الاسم على H2 فتفشل. مجلد
 * src/test/resources/db/migration-h2 يحوي نسخة منها بفارقين موثّقين في README بجانبها.
 * V7 نفسها مطبَّقة سلفًا في الإنتاج ولا يجوز تعديلها.
 *
 * هذا ليس بديلًا كاملًا عن Postgres حقيقي (أنواع وقيود قد تختلف)، لكنه يلتقط
 * الصنف الأخطر: عمود في كيان بلا هجرة تقابله، أو اسم/نوع عمود مختلف - وهو ما
 * يُسقط التطبيق عند الإقلاع في الإنتاج.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration-h2",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.url=jdbc:h2:mem:migverify;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
})
class MigrationsBuildValidSchemaTest {

    @Test
    void everyMigrationApplies_andHibernateValidatesTheResultingSchema() {
        // نجاح تحميل السياق هو التأكيد: Flyway طبّق V1..V12 ثم Hibernate تحقّق من المطابقة
    }
}
