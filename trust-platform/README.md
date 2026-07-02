# TRUST — المدير التجاري الذكي (MVP Scaffold)

هيكلة كاملة للمشروعين (Backend + Frontend) بأبسط نسخة شغالة end-to-end، متبوعة بخطة `trust_mvp_plan.md`.

```
trust-platform/
├── trust-backend/     Spring Boot API (Java 21, H2 in-memory, JPA)
└── trust-frontend/    React + TypeScript + Vite (لوحة القيادة الرئيسية)
```

---

## trust-backend

**التشغيل محليًا** (يتطلب Java 21 + Maven، وتحميل مكتبات Maven Central عند أول تشغيل):

```bash
cd trust-backend
mvn spring-boot:run
```

- يعمل على `http://localhost:8080`
- قاعدة بيانات H2 داخل الذاكرة، تُبذر تلقائيًا ببيانات تجريبية عند الإقلاع (`DataSeeder`) — مؤسسة "سوبرماركت النجمة" بفرع واحد وأرقام قريبة من التصميم المرجعي.
- لوحة H2 للمعاينة: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:trustdb`, user: `sa`, بدون كلمة مرور)

**أهم نقاط الـ API:**
```
GET  /api/dashboard?organizationId=1
GET  /api/items?branchId=1
GET  /api/items/needing-attention?branchId=1
POST /api/items
POST /api/entries/daily
GET  /api/recommendations?branchId=1
POST /api/recommendations/regenerate?branchId=1   # يشغّل محرك القواعد فورًا
```

> ⚠️ ملاحظة بيئة العمل الحالية: تعذّر تشغيل `mvn` فعليًا داخل بيئة التطوير هذه لعدم وجود وصول شبكي لـ Maven Central. الكود مكتمل ومُهيكل بالكامل (Entities → Repositories → Services → Controllers)، لكن التجميع والتشغيل الفعلي يحتاج بيئة عندها وصول لإنترنت عادي (جهازك المحلي أو أي CI).

**أهم الملفات لمراجعتها أولًا:**
- `service/HealthScoreService.java` — تطبيق معادلات مؤشر صحة الأعمال الست
- `service/RecommendationEngineService.java` — محرك التوصيات القائم على القواعد
- `seed/DataSeeder.java` — بيانات تجريبية

---

## trust-frontend

**التشغيل محليًا** (Node.js 18+):

```bash
cd trust-frontend
npm install
npm run dev
```

- يعمل على `http://localhost:5173`
- تم التحقق فعليًا: `npm install` و `npm run build` نجحا بدون أخطاء في هذه الجلسة.
- إن لم يكن الـ backend يعمل، تعرض الصفحة تلقائيًا بيانات تجريبية (`src/api/mock.ts`) بنفس شكل التصميم المرجعي حتى تبقى قابلة للمعاينة الفورية — مع تنبيه أصفر أعلى الصفحة يوضح أنها بيانات تجريبية.
- لربطها بالـ backend الحقيقي: شغّل `trust-backend` أولًا، ثم أعد تحميل الصفحة (أو اضبط `VITE_API_BASE_URL` في ملف `.env`).

**أهم الملفات لمراجعتها أولًا:**
- `src/pages/Dashboard.tsx` — تجميع الصفحة الرئيسية
- `src/components/*` — كل بطاقة/رسم بياني في مكوّن منفصل قابل لإعادة الاستخدام في باقي صفحات القائمة الجانبية
- `src/styles/theme.css` — نظام الألوان والـ RTL بنفس هوية التصميم الأصلي

---

## الخطوة التالية المقترحة

1. تشغيل `trust-backend` محليًا والتأكد من نجاح `mvn spring-boot:run` (سيبني قاعدة H2 ويبذر البيانات تلقائيًا).
2. ربط الفرونت اند بالـ backend الحقيقي بدل البيانات التجريبية.
3. بناء صفحة "المخزون" الكاملة (فورم إضافة صنف + جدول كامل) — البنية جاهزة في `ItemController` والمكونات.
4. بناء صفحة "إدخال البيانات اليومي" (نموذج المبيعات/الربح/السيولة) المرتبط بـ `POST /api/entries/daily`.
