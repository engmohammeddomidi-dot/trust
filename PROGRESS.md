# TRUST Platform — Progress Log

Living document tracking what's been built, what's left, and how to pick work back up. Update this whenever a work session ends or context is about to be compacted.

## Repo layout
- Git root: `C:\Users\m.domidi_freightos\Desktop\trust-platform`
- App code: `trust-platform\trust-backend` (Spring Boot 3.3.2 / Java 21) and `trust-platform\trust-frontend` (React 18 + TS + Vite)
- Original spec: `handover.md` at repo root
- Git repo initialized this project cycle. Remote: `https://github.com/engmohammeddomidi-dot/trust` (public), pushed 2026-07-04 — `master` is up to date through commit `de0a651` (market-grade batch + full Procurement Decision Engine + DataSeeder fix + combined Docker deployment). Push required accepting a pending collaborator invite via the GitHub API (`PATCH /user/repository_invitations/{id}`) since the cached git credential (`mohammedmahmouddomidi`) needed to be added as a collaborator on the repo (owned by `engmohammeddomidi-dot`) first.

## Deployment (2026-07-04) — combined single-service Docker build, PM demo target
Goal: let the PM test the app himself on a free-tier host. Decided on a **combined deployment** (frontend bundled into the backend, one process, one host) over separate FE/BE hosting, to minimize moving parts for a demo.

**What was built and verified:**
- **Postgres migration derisking**: ran the real `trust-postgres-test` Docker container (port 55432, db `trustdb`, user `trust`/`trustpass` — already existed from an earlier session) through all 5 Flyway migrations for the first time. They applied cleanly and Hibernate `ddl-auto=validate` confirmed the schema matches the JPA entities exactly — **the migrations themselves were never the risk**.
- **Real bug found this way**: `DataSeeder` ran unconditionally on every startup and crashed with a duplicate-key violation the moment it hit a database that already had data — which is guaranteed on the *second* start of any persistent (non-H2) deployment (a Render redeploy, a container restart, anything). Fixed: skips seeding entirely if `organizationRepository.count() > 0`. This would have caused a crash-loop on the very first redeploy in production had it shipped as-is.
- **Combined deploy mechanics**: `trust-frontend` gained a `build:bundled` npm script (`vite build --outDir ../trust-backend/src/main/resources/static`) so Spring Boot serves the SPA directly; `client.ts`'s API base URL now defaults to relative `/api` in production builds (falls back to `http://localhost:8080/api` only in `npm run dev`, unchanged); a new `SpaFallbackController` forwards React Router paths to `index.html` so deep links/refresh don't 404; `SecurityConfig`'s `anyRequest()` changed from `authenticated()` to `permitAll()` since the app shell must load publicly (real auth is unchanged, still enforced on `/api/**` server-side and client-side via `RequireAuth`); `server.port` now reads `${PORT:8080}` (Render/Railway/Heroku convention).
- **Dockerfile** (git root, multi-stage: Node build → Maven build → `eclipse-temurin:21-jre-alpine` runtime) + **render.yaml** Blueprint (`runtime: docker` — confirmed via Render's own docs that Java has no native runtime support, Docker is required, corrected this after initially telling the user otherwise) + `.dockerignore`.
- **Verified by actually building and running the Docker image locally** (not just reading the Dockerfile) against the real Postgres container over a Docker network — caught and fixed a real bug this way too: the entrypoint referenced `/app.jar` but the jar was at `/app/app.jar` (WORKDIR mismatch), which would have made the container crash-loop immediately on Render with no obvious cause from just reading the file. After the fix: confirmed health check, static HTML serving, JS asset serving, SPA deep-link routing, unauthenticated-API-still-401, and full login → authenticated dashboard call, all through the containerized app talking to real Postgres.

**What the user still needs to do (I cannot do this part — needs their dashboard access):**
1. In Neon/Supabase: create the Postgres database, get the connection string (host, port, db name, user, password).
2. In Render: create a new Blueprint from `render.yaml` (or a manual Web Service pointed at the Dockerfile) connected to `https://github.com/engmohammeddomidi-dot/trust`.
3. Set these env vars in Render's dashboard (all marked `sync: false` in render.yaml so they prompt during setup):
   - `POSTGRES_URL` = `jdbc:postgresql://<neon-host>:5432/<dbname>` (from step 1; note `SPRING_PROFILES_ACTIVE=postgres` is already set as a plain value in render.yaml, not a secret)
   - `POSTGRES_USER`, `POSTGRES_PASSWORD` = from step 1
   - `JWT_SECRET` = a generated value (any strong random string ≥32 chars works; one was generated this session — ask if it needs regenerating, don't reuse a value that appeared in shared chat history for a real production secret)
   - `CORS_ALLOWED_ORIGINS` = the Render-assigned URL itself (e.g. `https://trust-platform.onrender.com`) — harmless in a combined deploy since everything is same-origin, but the bean requires a non-empty value
4. Deploy, then smoke-test: login as `owner@trust.demo` / `password123` (seeded automatically on first boot against the fresh Neon DB), click through a few pages.
5. **Not yet done**: no CI runs this Docker build automatically — the GitHub Actions workflow mentioned earlier in this doc is still outstanding and would be the natural next step to catch build breaks before they reach Render.

## Governing instructions from the user (do not deviate without asking)
1. Keep working autonomously, don't stop, don't ask permission mid-task.
2. **"Market grade" push**: proceed with **all** improvements from the market-grade gap analysis **except monetization/billing**. The business model is group-order-driven revenue, not subscription/billing — so no payment, pricing tiers, or billing UI should be built.
3. User's own words on strategy: *"we dont want to add any monetization, the goal would be making money by haveing the group order through the system, but for that to be good, we need users, and to have users, the system should be good, you can proceed with all points other than 2"*
4. Verify everything by actually running the servers and testing via curl/browser — not just writing code and assuming it works. This has repeatedly surfaced real bugs (see "Bugs found & fixed" below).

## How to run
```bash
# Backend (from trust-platform/trust-platform/trust-backend)
export JAVA_HOME="/c/Users/m.domidi_freightos/.jdks/temurin-21.0.7"
export PATH="/c/Program Files/apache-maven-3.8.5/bin:$JAVA_HOME/bin:$PATH"
mvn.cmd -q -o spring-boot:run   # add -o only if no dependency changes; drop it to fetch new deps
# health check: curl http://localhost:8080/actuator/health

# Frontend — use the Claude Preview tool (preview_start with name "trust-frontend"), not raw npm run dev,
# so it's visible/controllable in this environment. Runs on :5173.
```
Demo credentials (H2 in-memory dev DB, reseeded fresh on every backend restart):
- Owner: `owner@trust.demo` / `password123` (org: سوبرماركت النجمة, branch: الفرع الرئيسي - رام الله)
- Platform admin: `admin@trust.demo` / `admin123`

A Postgres test container also exists for Flyway verification: `docker start/stop trust-postgres-test` (port 55432, db `trustdb`, user `trust`/`trustpass`).

## What's built — full feature inventory

### Core MVP (phase 1)
Dashboard with health-score gauge/radar, inventory management, daily entry, recommendations engine, notifications/alerts page, liquidity/profitability/pricing pages, sales history, purchases/suppliers/reports/settings pages, JWT auth + login page, daily recommendation scheduler, admin panel (org overview, stagnant items, benchmarks).

### Phase 2 (group purchasing + benchmarks)
Group purchasing backend+frontend (collect → negotiate → distribute flow), admin category benchmark management, simple clearance-matching view.

### Security/scalability hardening (mid-session audit)
- Tenant isolation via `TenantAccessGuard` (prevents IDOR across organizations) — audited all 11 controllers.
- CORS restricted from wildcard to configured origin list (`app.cors.allowed-origins`).
- Global exception handler (`GlobalExceptionHandler`) — consistent 400/401/403/404/409/429 mapping.
- N+1 query fixes in AdminController via batch repository methods (`findByOrganizationIdIn`, `findByBranchIdIn`).

### "Market grade" batch (most recent, answers "how does admin add new users?")
- **Admin creates tenant orgs**: `POST /api/admin/organizations` — creates Organization+Branch+OWNER user with a generated temp password, shown once in the UI (`CreateOrganizationModal`).
- **OWNER manages teammates**: `POST /api/users`, deactivate/activate — `AddUserModal` + team table in Settings.
- **Password reset**: token-based, in-app only (no SMTP yet) — reset link/token shown directly in the UI with an explanatory "no email service yet" notice.
- **JWT refresh tokens**: 15-min access token + 7-day opaque refresh token, rotated on each use, silent-refresh axios interceptor on the frontend.
- **Login rate limiting**: 5 failed attempts → 60s lockout (429), in-memory (`LoginAttemptService`).
- **Health-check + structured logging**: `/actuator/health`; `RequestLoggingFilter` stamps every request with a `requestId` (returned as `X-Request-Id` header, in MDC), logs one summary line per request; JSON structured logs (`logstash-logback-encoder`) auto-enabled under the `postgres` Spring profile, human-readable pattern otherwise.
- **Automated backend tests**: 15 JUnit/MockMvc tests — `SecurityAuthorizationIntegrationTest` (401/403/200 matrix), `AuthControllerIntegrationTest` (login/lockout), `ItemBulkImportIntegrationTest` (CSV validation + IDOR), `TenantAccessGuardTest` (unit). All passing.
- **CSV bulk import** for items, with per-row manual validation (partial success + descriptive errors).
- **Audit log**: `AuditLogService.record(...)` called at key mutation points, visible to OWNER in Settings.
- **Full data export**: `GET /api/data-export` (OWNER-only) returns all tenant data (branches, users, items, daily entries, purchases, recommendations) as one JSON bundle; "⬇ تصدير جميع البيانات" button in Settings triggers a browser download. Fulfills the data-portability promise in the ToS text.
- **Terms of Service acceptance flow**: non-dismissable `TosGateModal` shown on first login post-signup (gated by `User.tosAcceptedAt`), blocks app access until accepted.
- **In-app notification center**: `NotificationBell` (unread badge + dropdown) — substitute for real email/WhatsApp delivery, wired into `RecommendationEngineService` (fires on new HIGH-priority recs) and group-order state changes.
- **PostgreSQL + Flyway migration**: `V1__init_schema.sql` covers all 16 entities with FK constraints and explicit indexes (Postgres doesn't auto-index FK columns). Verified end-to-end against a real Postgres container: Flyway migrates cleanly, Hibernate `ddl-auto: validate` confirms the schema matches the JPA model exactly, full API smoke-tested against it.

### Procurement Decision Engine — full build (backend + frontend, 2026-07-03/04)
Started as a single vertical slice ("Issue Purchase Order" only), then extended through the full loop per explicit user instruction ("proceed, keep going till all tasks are done"). Implements most of the PM-vision initiative below, scoped down where noted.

**Backend — data model**
- **`Supplier`** (`domain/Supplier.java`): name, contactInfo, `leadTimeDays`, `creditTermsDays`, `rating` (0–100), scoped to an Organization. CRUD via `SupplierController`/`SupplierService` (`GET/POST /api/suppliers`, `PUT /api/suppliers/{id}`).
- **`Item` extended**: `supplier` (optional preferred supplier, FK) and `safetyStockDays` (default 3). Linked via `PATCH /api/items/{id}/supplier`.
- **`Decision`** (`domain/Decision.java`): `reasonSummary` (explanation), `confidenceScore` (0–100), `financialImpact`, `status` (OPEN/APPROVED/MODIFIED/DEFERRED/DISMISSED), `suggestedQuantity`/`approvedQuantity`, `actualOutcome`/`outcomeRecordedAt` (now actually populated — see Purchase lifecycle below). `type` enum currently has only `PURCHASE_ORDER`; extensible.
- **`Policy`** (`domain/Policy.java`): one row per organization — `maxPurchaseLiquidityRatio` (default 0.25) and `minSupplierRating` (default 0). Replaces the hardcoded liquidity-cap constant from the first slice. CRUD via `PolicyController` (`GET/PUT /api/policies`).
- **`Goal`** (`domain/Goal.java`): one row per (organization, type) for 7 goal types (`INCREASE_PROFITABILITY`, `IMPROVE_LIQUIDITY`, `PREVENT_STOCKOUTS`, `REDUCE_STAGNANT_INVENTORY`, `INCREASE_SALES`, `IMPROVE_SUPPLIER_PERFORMANCE`, `INCREASE_INVENTORY_TURNOVER`), priority 1–5 (default 3 = neutral). CRUD via `GoalController` (`GET/PUT /api/goals`). **Only 2 of the 7 goals actually influence engine behavior today** (see below) — the rest are stored but inert, honestly reflecting that the other analysis engines (sales/profit/supplier-performance) from the PM's 8-engine vision don't exist yet. Adding real effects for the remaining 5 requires building those engines first, not just wiring a number.
- **`Purchase` extended** with a real order lifecycle: `status` (SENT/RECEIVED, default RECEIVED for old manual entries so existing behavior is unchanged), `supplier`/`decision` FKs, `receivedQuantity`, `receivedDate`, `priceMatched`, `hasDamage`, `hasDiscrepancy`.

**Backend — decision engine logic** (`PurchaseDecisionEngineService`)
- Days-of-coverage vs. `leadTimeDays + safetyStockDays` (shared `SalesEstimator` heuristic with `RecommendationEngineService`, extracted so the two engines can't disagree).
- Liquidity gate now reads `Policy.maxPurchaseLiquidityRatio` per organization instead of a constant.
- **Goal-driven behavior** (the only two goals wired to real logic): `PREVENT_STOCKOUTS` priority adds/removes `(priority - 3)` days to the effective safety-stock buffer; `IMPROVE_LIQUIDITY` priority tightens/loosens the effective liquidity ratio by `10% × (priority - 3)`. Both are reflected in the plain-language `reasonSummary` shown to the user (e.g. "شمل 2 يوم إضافي بسبب أولوية منع نفاد المخزون").
- **Policy-driven supplier check**: if the item's linked supplier's rating is below `Policy.minSupplierRating`, the decision still generates (doesn't silently swap suppliers) but flags it explicitly in the reason text and drops confidence by 25 points — matches the PM's "لا تعتمد موردًا يقل تقييمه... إلا بموافقة المدير" (needs manual approval, isn't auto-blocked).

**Backend — PO lifecycle** (`DecisionActionService`, closes the loop the PM described: "الاعتماد لا ينهي العمل، بل تبدأ المتابعة")
- Approve/Modify now create a real `Purchase` record (status `SENT`) linked to the `Decision` — this didn't exist in the first slice (approving only flipped a status enum before).
- New `PATCH /api/purchases/{id}/receive` (body: receivedQuantity, priceMatched, hasDamage): flips the purchase to `RECEIVED`, **actually increments `Item.quantity`** by the received amount, writes a human-readable `actualOutcome` onto the linked `Decision` (fulfilling the PM's "measure" stage), and nudges the supplier's `rating` (+1 for a perfect match, −3/−2/−5 for quantity mismatch/price mismatch/damage respectively, clamped 0–100).
- New `GET /api/decisions/quality-score`: % of received orders with zero discrepancy — the PM's "Decision Quality Score" concept, computed from real outcomes rather than a fabricated number.
- Existing manual purchase entry (`POST /api/purchases`, used by the old "تسجيل شراء" flow) is untouched — new orders default to `RECEIVED` status so nothing about that flow changed.

**Frontend** (new — the first slice had none)
- **Suppliers page rebuilt**: real Supplier CRUD (create/edit modal with lead time, credit terms, rating) alongside the pre-existing purchase-history-derived summary and group-order sections (kept, not removed).
- **Inventory page**: added "المورد المفضّل" (preferred supplier) dropdown and "مخزون الأمان" (safety stock days) input per row, wired to `PATCH /api/items/{id}/supplier`.
- **New "قرارات الشراء" (Purchase Decisions) page + nav item**, the closest thing to the PM's "Decision Center" concept built so far: tabbed (بانتظار قرارك / معتمدة بانتظار الاستلام / السجل), each decision as a card showing reason + confidence + financial impact with inline Approve/Modify/Defer/Dismiss, and an inline Receive form (quantity/price-match/damage) once approved — this is the PM's explicit "5-element decision card" (recommendation, reason, financial impact, confidence, one-tap action), just not yet the full home-screen redesign.
- **Settings page**: added "سياسات محرك القرار" (Policy) and "أولويات العمل" (Goals, with ⭐ 1–5 selectors) sections, OWNER-only.
- Global `Header`/quality-score badge on the Decisions page shows the live score computed above.

**Verified — backend tests, live curl, AND interactive browser** (not just code review or compilation):
- Full JUnit suite (15 tests) green throughout.
- Killed and restarted the dev backend after every batch of changes; curl-verified: Policy/Goal CRUD, goal-driven safety-day and liquidity-ratio math showing up correctly in regenerated decisions, approve → real `Purchase(SENT)` created, receive → item quantity +850/+233 confirmed, supplier rating +1 (perfect) and −10 (triple penalty: quantity+price+damage) confirmed, decision quality score 100% → 50% → 66.7% tracked correctly across three receipts, double-receive correctly rejected with 409, bogus supplier id correctly rejected with 400.
- **Full interactive browser pass** via the preview tool: logged in, accepted ToS, exercised Suppliers CRUD (including a live edit that fixed a corrupted-Arabic test row — see note below), linked a supplier to an item from Inventory and watched the row update, opened the Decisions page and drove Modify (quantity + supplier change) and Receive (with live quality-score badge updating 50%→66.7% in the UI) end-to-end, confirmed Settings correctly displays Policy/Goal values that were set via the API. Zero browser console errors throughout.
- **Encoding note (not a real bug)**: testing Arabic payloads via inline `curl -d '...'` args in this Windows/git-bash environment corrupts non-ASCII bytes before they reach curl.exe (confirmed this affects the pre-existing `ItemController` too, not just new code) — a Windows argv-encoding artifact of this specific shell setup, not the backend or frontend. Confirmed the real path (browser → axios → backend) round-trips Arabic perfectly; use `curl --data-binary @file.json` for any future curl-based Arabic testing in this environment.

**Known gaps / explicitly deferred** (honest scope boundary, not "done"):
- No Decision Center home-screen redesign — the Dashboard still shows the old metrics-first layout; only a dedicated Decisions page exists.
- Only `PURCHASE_ORDER` decisions exist. Pricing, promotions, supplier-switching, and clearance decisions from the PM's vision are not modeled.
- 5 of 7 Goals are stored but don't affect any engine yet (no engine exists for them to influence).
- No "Trust Advisor" persona/messaging, no scenario/alternatives generation (the PM's "Option A/B/C" concept), no automated Measure/Learn beyond the receive-triggered outcome text.
- **`V2`/`V3`/`V4`/`V5` Postgres migrations (suppliers, decisions, PO-lifecycle columns, policies, goals) have not been verified against the real Postgres test container** — only checked for syntactic consistency with `V1`'s style and confirmed the H2 dev profile (`ddl-auto: update`) picks up the new entities correctly. Do this before relying on the `postgres` profile.

## Bugs found & fixed this session (all via actual testing, not code review)
1. **CSV bulk import silently accepted invalid rows** (e.g. negative cost price) — `@Valid` doesn't cascade into `List<T>` fields automatically. Fixed with explicit manual per-row validation in `ItemController.bulkImport()`.
2. **Rate limiting returned 403 instead of 429** — needed a dedicated `TooManyAttemptsException` + explicit `@ExceptionHandler` in `GlobalExceptionHandler` (Spring's `ResponseStatusException` wasn't resolving correctly here).
3. **Critical own-mistake**: accidentally used `Write` (full overwrite) on `client.ts` instead of `Edit`, destroying ~380 lines of existing API functions. Recovered via `git show HEAD:...`. Lesson: only use `Write` for brand-new files or deliberate full rewrites.
4. **Unauthenticated requests returned 403 instead of 401** — Spring Security's default `AuthenticationEntryPoint` behavior. Fixed by adding a custom `AuthenticationEntryPoint` bean in `SecurityConfig`.
5. **That fix then broke role-based 403s** (valid token + wrong role started returning 401 too) — root cause: the entry point/access-denied-handler used `response.sendError()`, which triggers Servlet container's `/error` re-dispatch; that re-dispatch runs back through the *entire* Spring Security filter chain a second time, and the second pass was resolving to "unauthenticated" and clobbering the original 403. Fixed by switching both the `AuthenticationEntryPoint` and `AccessDeniedHandler` to `response.setStatus(...)` directly (no re-dispatch). Verified with a debug-logging trace showing the double filter-chain execution before the fix. **This is now regression-tested** in `SecurityAuthorizationIntegrationTest`.
6. **Login rate limiting never triggered for non-existent emails** — `LoginAttemptService.recordFailure()` was only called on the wrong-password branch; a request for an email that doesn't exist at all threw `BadCredentialsException` before ever recording a failed attempt, so unlimited enumeration attempts against nonexistent accounts were possible. Fixed in `AuthController.login()` to record failure in both branches. Found via a rate-limit integration test that used a fresh (nonexistent) email and got a surprising 401-instead-of-429 on the 6th attempt.

## Status of the market-grade task list (see conversation task list for full IDs)
All items complete **except**:
- **GitHub Actions CI workflow scaffolding** — in progress when this note was written. Plan: `.github/workflows/ci.yml` at the git root (`C:\Users\m.domidi_freightos\Desktop\trust-platform`) with two jobs — backend (`mvn test`, needs JDK 21 setup) and frontend (`npm ci && npx tsc -b --noEmit && npm run build`). Need to confirm actual working-directory paths relative to git root (backend/frontend are nested one level under `trust-platform/trust-platform/`, not directly under git root — verify with `ls` before writing paths into the workflow) and whether a GitHub remote exists yet (`git remote -v`) before assuming the workflow will actually run anywhere.

Not in scope per user instruction: monetization/billing (item 2 of the original gap analysis).

## Procurement Decision Engine — new PM-driven initiative (Phase 3)

PM sent a long series of strategy notes (2026-07-03) reframing where TRUST goes after the current MVP/market-grade base. Condensed synthesis, in priority order:

- **Product philosophy shift**: the app should not present data/lists, it should present *decisions*. Home screen question becomes "ماذا يجب أن أفعل اليوم؟" (What should I do today?) instead of a metrics dashboard — a "Decision Center," not a "Dashboard."
- **Purchasing pre-question**: before "what/where/when/how/how much to buy," the engine must first answer "should I buy at all?" — i.e. every recommendation needs a real trigger (reorder point, risk, opportunity), never a habitual/rep-visit-driven purchase.
- **Explainable AI is non-negotiable**: every recommendation card must always show 5 fixed elements — the recommendation, the reason, the financial impact, a confidence score, and an immediate action button (Approve/Modify/Defer). No black-box output.
- **Decision Life Cycle** (applies to every decision type, not just purchasing): Monitor → Detect → Analyze → Generate Options (multiple scenarios, not one) → Recommend → Execute → Measure → Learn. Includes a post-execution "Decision Quality Score" that closes the loop (did the recommendation actually pan out?).
- **Layered architecture** the PM wants reflected in the build order, top to bottom: Business Goals → Business Policies (per-tenant configurable rules, e.g. "never let liquidity-critical stock run out," "reject any supplier below 80 rating") → Analysis Engines (sales, inventory, procurement, supplier, cash-flow, profit, market) → one unifying Decision Engine → screens/execution/follow-up. This matches (and should extend, not replace) the existing rule engine in `RecommendationEngineService` — the new piece is the Policy layer and the multi-engine consolidation, not a rewrite of what's already shipped.
- **Data model additions implied**: `Decision` entity (type, reason, confidence, financial impact, execution status, actual outcome — for the learning loop), `Policy` entity (tenant-configurable rule thresholds, replacing hardcoded constants like the 60-day stagnation cutoff), `Goal` entity (tenant-ranked business objectives that reweight recommendation priority). These extend the existing `recommendations` / `category_benchmarks` tables rather than being a parallel system.
- **PM's own recommended next step** (from the last message, and the one to actually act on first): don't jump to new screens. Design the **data model for a single decision type first** — "Issue Purchase Order" — fully: every input field, its source, whether it's required, and how the decision engine consumes it. Use that as the reference pattern before extending to other decision types (pricing, promotions, supplier switching, clearance).
- **MVP-sized slice the PM proposed** if/when this gets built: 10 screens — Decision Center (home), Daily Tasks, Purchase Recommendations, Supplier Comparison, Order Review/Approval, Order Tracking, Goods Receipt, Supplier Rating, Performance Indicators, Settings/Policies. This is explicitly scoped smaller than the full 8-engine/12-entity vision — the PM's stated strategy is to validate with a handful of real supermarkets before expanding, not to build the whole architecture upfront.

**Update 2026-07-03**: user explicitly said to skip the spec/sign-off step and implement directly ("implement only, dont waste time on docs and specs"), then explicitly said to keep going until all tasks are done ("yes proceed, keep going till all tasks are done"). What actually shipped is captured in full in "Procurement Decision Engine — full build" earlier in this doc, including an honest "known gaps" list — it is a real, working, end-to-end reference implementation of the *purchase-order* decision type (data → engine → policy/goal influence → approve → real order → receive → inventory update → supplier rating → outcome → quality score → UI), not the entirety of the PM's 13-message vision (that would mean 6+ decision types, a Decision Center home-screen redesign, a full 8-engine architecture, and scenario-based recommendations — explicitly out of scope for this pass and listed as such).

## Next steps when resuming
1. Finish the CI workflow (see plan above).
2. Do a final `git status`/`git diff` review and commit the market-grade batch **and** the full Procurement Decision Engine batch (neither committed yet as of this note — confirm with the user before committing, and run through repo conventions first if any exist, e.g. check for a CLAUDE.md/CONTRIBUTING doc in the trust-platform repo itself, separate from the general Playwright CLAUDE.md that doesn't apply here).
3. Both dev servers (backend :8080, frontend :5173 via Claude Preview) should be left running for the user unless told to stop them. The backend was restarted twice this session (final restart at the end, after all Policy/Goal/PO-lifecycle code landed) — it's running the current code as of this note.
4. Verify the `V2`–`V5` Postgres migrations against the real Postgres test container (`trust-postgres-test`) the way `V1` was verified — not done yet, see the known-gaps note above.

## Mockup-driven UI rebuild — 10-milestone plan (2026-07-21/22)

After the app was deployed (combined Docker image, Neon Postgres, Render — see prior session notes not duplicated here), the PM shared a `TRUST-VISION-ANALYSIS.md` document plus 3 new UI mockups (Supplier Portal, Platform Admin Dashboard, Owner/Merchant Dashboard). User asked to match the mockups' logic/design to the earlier vision analysis, split into 10 milestones, and build them one by one. Explicit choices made when asked: (a) build a real light/dark theme toggle using the frontend-design skill, not a one-off reskin; (b) do the Supplier Portal milestones last, after Owner and Admin dashboards.

**M1 — Theme system**: `src/theme/theme.ts` (localStorage-persisted `trust-theme` key, applied pre-render in `main.tsx` to avoid a flash), `ThemeToggle.tsx`, `theme.css` restructured into `:root` (shared tokens) / `:root,[data-theme='dark']` (existing values, unchanged) / `[data-theme='light']` (new values from the mockups). Zero component changes needed — all existing CSS already used variables.

**M2–M5 — Owner Dashboard rebuild**: daily KPI row, a real performance/impact gauge + resolved-risk/opportunity counts (`DecisionAnalyticsService.performanceImpactSummary`), a Monthly Impact Ledger backed by a genuinely new mechanism (`HealthScoreService.snapshotToday()` populating the previously-unused `health_score_history` table — that table existed since the MVP schema but nothing had ever written to it), and an Executive Action Center (top-3 items by profitability/accumulated dead-stock cost, real alert counts, top-3 open decisions as a recommendations feed). `Decision` gained a `category` (RISK/OPPORTUNITY) field computed from a real signal (`daysCoverage <= leadTimeDays`), not an arbitrary label.

**M6–M8 — Admin Dashboard rebuild**: platform KPI row + a real 7-day platform-wide sales trend (`DailyEntry` aggregated across every org/branch) + category breakdown + a city-level breakdown table. **Note**: the mockup's "map" view has no real backing data — `Organization`/`Branch` have no lat/lng or region field, only a free-text `city` string — so a city-grouped table shipped instead of a fabricated map, and this substitution was flagged to the user rather than silently done. Also added: open risk/opportunity counts + financial impact platform-wide, a health-score distribution (good/medium/poor bands), a top-5 organization leaderboard, a platform-wide performance gauge (confirmed `DecisionAnalyticsService.performanceImpactSummary()` has no tenant-specific logic baked in, so it's safe to call with every branch ID on the platform rather than one org's), and a cross-tenant top-5 recommendations feed (highest-financial-impact open decisions across all orgs, surfacing org/branch/item names via the `Decision → Branch → Organization` association chain).

**M9–M10 — Supplier Portal (new)**: a genuinely new capability — one supplier company can serve many different tenant organizations, which cuts against how every other role in this app is tenant-isolated. Design: `Supplier` gets a nullable `email` field; a new `SUPPLIER` role on `User` (with `organization=null`/`branch=null`, the same pattern already used for `PLATFORM_ADMIN` — required zero `User` schema change); a supplier's identity is resolved purely by matching `principal.email()` against `Supplier.email` across every org at query time, no new join table. New `/api/supplier/**` endpoint group gated by `hasRole("SUPPLIER")`; no new tenant guard needed since every query is scoped by the caller's own email, not a client-supplied id. New admin endpoint to issue a supplier login (mirrors the existing `createOrganization` temp-password pattern). Seeded the *same* real supplier ("شركة الأمين للتوريدات") into both demo organizations under one email, with one open and one received purchase order, to prove the cross-tenant aggregation is real rather than demoed with fabricated data.

**Verification discipline held throughout**: every milestone — backend `mvn test` (stayed at 15/15 green throughout, now 15 tests still, no new failures introduced by 10 milestones of changes), frontend `tsc --noEmit` + `vite build`, a live-restarted backend + hand-computed curl cross-checks (e.g. M9: avgRating 88.5 confirmed as the exact average of the two orgs' own supplier ratings 92/85; expectedDeliveryDate confirmed as `purchaseDate + that org's own supplier.leadTimeDays`), and browser verification via `read_page`/`get_page_text` (screenshot capture was flaky this session in the browser tool itself — confirmed via `get_page_text` and DOM reads instead, plus a fresh-tab console check to rule out stale accumulated console-buffer entries, which recurred across M7/M8/M9 checks with an identical stale timestamp and were correctly identified as non-issues each time).

**Postgres migration gap closed**: ran the backend against a real Postgres 16 container (not just H2 `ddl-auto=update`) with the `postgres` Spring profile and confirmed Flyway applies `V1`–`V7` cleanly in sequence — this had been an open gap since `V2`–`V6` were added without ever being run together against real Postgres.

**Known gaps after M1–M10** (honest, not fabricated as "done"):
- Supplier Portal is genuinely a *foundation* — one overview page, no order-acceptance/rejection actions, no supplier-side rating visibility beyond the aggregate number, no notifications when a new PO is issued to them.
- Admin Dashboard's "map" is a city-grouped table, not a real geographic map (see M6–M8 note above) — would need a real lat/lng/region field added to `Branch` to become a true map, which is a schema change not yet made.
- 5 of 7 `Goal` types are still inert (unchanged from the Procurement Decision Engine phase — no new engines were added this phase to give them real effect).
- The chunk-size build warning (`vite build` — main bundle ~780KB) was not addressed; no code-splitting has been introduced.
5. If continuing the Procurement Decision Engine work, the highest-value next pieces per the PM's own emphasis are: (a) a real Decision Center home-screen ("ماذا يجب أن أفعل اليوم؟") that surfaces open decisions from the Dashboard instead of requiring a separate page visit, (b) a second decision type (pricing is the most natural next one, since `RecommendationEngineService.ADJUST_PRICE` already has the underlying logic — the work would be porting it to the `Decision` model's explainability format), (c) only build out the remaining 5 Goals once their corresponding analysis engines exist — don't wire numbers to nothing.

## BHI — Business Health Index rebuilt from the PM's reference model (2026-08-28)

The PM sent two files: an xlsx defining a formal **BHI (Business Health Index)** methodology, and a screenshot of a single-store unit-economics/revenue model. Only the xlsx was actioned this session (user: "focus on the first part only", then "improvise where needed" — so the open questions below were decided rather than escalated).

**The scoring math was reverse-engineered exactly, not approximated.** Normalization is piecewise-linear between three anchors — weak→40, medium→70, excellent→100, extrapolating below weak, capped at 100 above excellent; direction (`أعلى أفضل`/`أقل أفضل`) flips the interpolation. Aggregation is two-level: **equal weight within an axis** (the sheet's "الوزن داخل المحور" column actually holds the direction text, not a weight — confirmed by arithmetic) and **explicit weights across axes** (0.30/0.20/0.20/0.15/0.15). All 13 indicators plus the overall 77.208451 and every axis score reproduce to 4 decimals; these are locked in as golden tests (`BhiScoringEngineTest`, `BhiAggregationTest`).

**What shipped**
- `BhiScoringEngine` — pure, no DB access, so it can be verified literally against the reference sheet. Also does multi-branch averaging.
- `BhiMetricsCalculator` — derives raw indicator values; every one returns `null` (not 0, not Infinity) when its data is missing.
- `BhiService` — repositories → raw inputs → scores; thresholds/weights read from sparse override tables.
- `BhiThreshold` / `BhiAxisWeight` + `V8` — **override-only tables**, no seed rows. Defaults live on the `BhiIndicatorCode`/`BhiAxis` enums (same `orElseGet` pattern as `CategoryBenchmark`), so a new `Category` works with zero rows instead of needing 13 × N seeds.
- `GET /api/bhi?branchId=` — full breakdown with a plain-language explanation per indicator.
- Frontend: `HealthRadar` now maps over server-supplied axes (no hardcoded six), new `BhiBreakdown` drill-down, `HealthGauge` renders `—` for an uncomputed score.

**Cutover — one health number, deliberately.** Three call sites now serve BHI: `DashboardService.healthScore`, `HealthScoreService.snapshotToday` (writes the BHI total into `HealthScoreHistory.totalScore`), and **`AdminController.avgHealthScore`**, which feeds the platform KPI, the city breakdown, the org leaderboard and the health distribution. The admin one was nearly missed — grepping `HealthScoreHistory` showed only `totalScore` was read, but `AdminController` calls `healthScoreService.calculate(...)` *live*, bypassing history. Had it been left, platform admins would have seen legacy 6-axis scores while owners saw BHI. `AdminHealthDistributionDto`'s band cuts also moved from the retired 61/41 to BHI's 70/55 so they match `BhiScoringEngine.classify`.

Verified live that the two agree: owner dashboard `93.88931683479217` ≡ `GET /api/bhi` for the same branch, and the admin leaderboard shows `93.9` for that org. `HealthScoreService.calculate` still exists for `RecommendationSchedulerService`. **The trend line will show a one-time step at cutover** — the two methodologies don't agree; accepted because the series is only weeks old.

**Improvised decisions** (reference model was silent; PM should confirm):
1. **Classification bands** — sheet gives one point (77.21 → "جيدة"). Chose cuts from the same anchors as the scoring scale: ≥85 ممتازة, ≥70 جيدة, ≥55 مقبولة, else ضعيفة.
2. **Below the weak threshold** — no example in the sheet exercises it. Linear extrapolation at the weak→medium slope, floored at 0.
3. **A real inconsistency in the sheet's inputs** — inventory days (38) and turnover (7.5, i.e. 48.7 days) are entered independently and contradict each other. Here both derive from the same COGS/inventory data so they always agree; CCC = inventoryDays + DSO − DPO.

**Two defects found by live testing, not code review** (the discipline paid off again):
1. **Period length was calendar days, not recorded days.** The demo branch has 7 daily entries in a 30-day window; dividing by 30 assumed 23 zero-sales days, understating the daily rate ~4× and inflating DSO 3.65→15.65 and DPO. Now uses entry count, matching the convention already in `HealthScoreService.calculatePurchasesScore`. Sales growth also switched to comparing **daily rates**, so unequal window lengths don't read as a sales collapse.
2. **An axis with no data vanished from the response entirely.** Real case: `OPERATIONAL_EFFICIENCY` disappeared because no prior-period entries exist, so the owner couldn't tell a whole axis went unassessed. `AxisScore.score` is now nullable — the axis is listed as `غير مُقيَّم`, still excluded from the weighted total.

**Data gap — 7 of 13 indicators computable today** (verified live: the demo branch scores on 6, since sales growth needs a prior period):
- *Available now*: gross margin, current ratio, cash ratio, CCC, sales growth, inventory turnover, DSO.
- *Needs a `MonthlyExpense` entity*: net profit margin + opex ratio. Highest priority — الربحية carries weight 0.30 and currently rests on one indicator. Note `DailyEntry.totalProfit` is **gross**; net is not a formula tweak. The PM's screenshot already specifies this input (manager 5000, 3× shelf staff 6000, electricity 2500, rent 3000, tech 200, sundries 100 = 16,800) and it cross-checks: those figures give exactly the 4% net margin and 0.16 opex ratio in the xlsx.
- *Needs new features*: waste log (نسبة الهدر), physical stock count (دقة الجرد), equity field (نسبة الدين). `PAYMENT_EFFICIENCY` needs only `dueDate`/`paidOn` on `Purchase` — cheap.

**Verification**: 84/84 backend tests green (was 15; +69 BHI). `tsc --noEmit` clean, `vite build` clean. Live-verified end-to-end against a restarted backend — every number hand-recomputed from the API's own inputs (turnover 371.3 = 400,617.27 × 365/7 ÷ 56,260; current ratio 0.77 = (415,680+56,260+312,400)/1,019,080; DSO 3.65; CCC −13.17).

**Not verified / known gaps**
- **V8 was NOT run against real Postgres** — Docker daemon was down. Mitigation: `BhiMigrationMatchesEntitiesTest` binds every Hibernate-derived column to the V8 script, and was proven to fail when a column name is altered. Still, run the real container before relying on the `postgres` profile.
- **Full Flyway-on-H2 validation is impossible**: `V7` hardcodes `app_users_role_check`, a constraint name Postgres generates and H2 does not. V1–V6 apply cleanly on H2. Worth fixing V7 to make that check runnable.
- **Demo data makes turnover absurd (371×/year)** — 6 items, 56K inventory against ~400K monthly COGS. The math is right; the seed data isn't shaped like a real supermarket. Seeding realistic inventory depth would make the dashboard demo honest.
- No admin UI yet for editing BHI thresholds/weights — the tables and the override lookup exist, `AdminBenchmarksPage` isn't wired to them.
- The screenshot (unit economics / 4 commission streams) is untouched. Its بند 1 savings taxonomy is a spec for the Impact Ledger and بند 3 is supplier-rebate revenue — both buildable; the 30% commission + before/after **subscription** P&L conflicts with the standing no-monetization rule and needs a decision.

### Follow-ups from the review pass (same session)

Two more defects, both caught by checking rather than assuming:

3. **Zero sales read as missing data.** `SALES_GROWTH` returned `null` when current sales were 0, because the rate helper treated a zero *numerator* as unavailable. A total sales collapse — the loudest signal that axis exists to catch — was being reported as "غير متاح". Now only a zero *denominator* means unavailable; a collapse reads as −100%.
4. **A dead parameter.** `assembleInputs(..., int calendarDays)` stopped reading that argument once the period switched to entry counts. Removed rather than left to mislead.

**Process note worth remembering:** a stale JVM served two verification rounds. An earlier `taskkill` sat after a `timeout`-ed maven command that exited 143, so it never ran; port 8080 stayed bound and the "restarted" backend silently failed to bind while the old process kept answering. It was caught only because the admin health distribution (good=1, medium=1) was arithmetically impossible under the new 70/55 bands for scores of 64.1 and 55.2 — it matched the old 61/41 cuts exactly. **When a live check disagrees with the source, confirm which process is actually answering before editing anything.**

**Demo-data caveat, now visible on the dashboard:** both seeded orgs score 93.9 and 100.0, and `dailyPerformanceSummary.inventoryTurnoverRatePercent` jumped from ~65 to 100. Nothing is wrong with the math — the seed has 6 items / ~56K inventory against ~400K monthly COGS, so turnover (371×/year) pins every inventory indicator at the ceiling. Seeding realistic inventory depth is now the single highest-value fix for making the demo credible.

## BHI phase 2 — all 13 indicators live (2026-08-28, same session)

User asked to carry the remaining plan to completion. Everything listed as "planned" for BHI is now built, wired, and verified; **13 of 13 indicators compute on real data** where before it was 7.

### New data sources (each was a "غير متاح" indicator)

| Source | Unlocks | Notes |
|---|---|---|
| `MonthlyExpense` + `/api/expenses` (V9) | هامش صافي الربح، نسبة المصاريف | Quantity × unit-amount mirrors the PM's expense table (3 shelf staff × 2000) so the shopkeeper doesn't multiply in their head |
| `WasteRecord` + `/api/waste` (V10) | نسبة الهدر | Recording waste **deducts** the quantity from the item — otherwise the book stays above reality |
| `StockCount` + `/api/stock-counts` (V10) | دقة الجرد | A count **corrects** book to counted, else the same variance repeats every count |
| `Purchase.paymentDueDate` / `paidOnDate` (V10) | كفاءة السداد | |
| `Organization.equity` (V10) | نسبة الدين إلى حقوق الملكية | Nullable — blank keeps the indicator honestly unavailable |

### The proration subtlety worth remembering

Operating expenses accrue on **calendar** days, but the BHI period is measured in **recorded-entry** days. Comparing a full month of expenses against a week of sales would show a fake negative net margin. `MonthlyExpenseService.proratedForPeriod` scales by `recordedDays / 30`. Reference check: 105,000 sales / 16,800 expenses = 0.16; at 7 recorded days that's 24,500 / 3,920 — still exactly 0.16.

### Judgement calls in the new indicators

- **An unpaid invoice past its due date counts as late, not as missing data.** Excluding it would make a defaulter look punctual purely for never paying. An invoice not yet due *is* excluded — its outcome is genuinely unknown.
- **A stock count with zero variance scores 100%, not "unavailable".** No count at all is missing data; a clean count is excellent performance. Different things.
- **Waste with no inventory to measure against** returns null rather than dividing by zero.

### Admin calibration — `/admin/bhi-config`

New page + `AdminBhiConfigController` (PLATFORM_ADMIN only, verified an OWNER gets **403**). Edits both indicator thresholds and axis weights per `Category`, marks each value معدَّل vs افتراضي, and can reset to the reference model. Warns when axis weights don't sum to 1. This is what turns the model from constants-in-code into something the product team owns — a pharmacy's "excellent turnover" is not a supermarket's.

Verified live: raising the turnover excellent bar 12 → 20 moved that indicator 80.37 → 73.46 **and** changed its explanation text; reset restored 80.37 exactly.

### Demo seed rebuilt to the actual target persona

The old seed did ~85,000 ₪/day — a hypermarket, not the corner shop this product is for — with 33.1% gross margin (above every "excellent" threshold) and 6 items worth 56K against ~400K monthly COGS. **That is why every indicator pinned at 100.** Rebuilt:

- ~3,500 ₪/day (~105,000/month), matching the PM's own unit-economics model
- 22% gross margin (realistic for a grocery; 33.1% was unreachable-high)
- **60 days** of daily entries, not 7 — BHI compares the last 30 to the prior 30, so 7 days left sales-growth permanently unavailable
- Inventory ~110K at cost → turnover ~9×/yr, while rice/olive-oil stay low so the purchase-decision engine still fires
- Expenses, waste, stock counts, purchase due/paid dates, and equity all seeded
- Purchases spread across 25 days so both the dashboard's 7-day window and the detail 30-day window see invoices

Result: BHI **78.07** over 7 days and **82.04** over 31 days, 13/13 both, with a genuine spread (68.70–100) instead of a wall of 100s.

### Verification

105/105 backend tests (was 15 at session start; +90). `tsc --noEmit` and `vite build` clean. Live-verified end to end, including that **writes move the right axis and only that axis**: recording waste moved إدارة المخزون 74.99 → 74.63 leaving الربحية untouched; raising rent 3000 → 6000 moved الربحية 86.83 → 84.54 leaving إدارة المخزون untouched. Browser-verified the dashboard radar (5 axes), the drill-down (scores + explanations + a greyed unavailable indicator showing its reason), the inventory waste/count card, the Settings expense table, and the admin calibration page. Zero console errors.

### Still open

- **No migration has been run against real Postgres this session** — the Docker daemon is down. `BhiMigrationMatchesEntitiesTest` now binds every Hibernate-derived column of all five new tables (plus the three added columns) to V8/V9/V10, and was proven to fail when a column name drifts — but that is not the same as applying the migrations. **Run the container before deploying.**
- Full Flyway-on-H2 validation remains impossible because **V7 hardcodes `app_users_role_check`**, a constraint name Postgres generates and H2 does not. V1–V6 apply cleanly on H2. V7 must not be edited (it is already applied in the deployed Neon database — changing it would break Flyway's checksum), so the fix is a new migration that normalises the constraint, not an edit.
- The screenshot's unit-economics model is still untouched; the 30% commission / before-after **subscription** P&L still conflicts with the standing no-monetization rule and needs your decision.
- ~~Second demo org was not rescaled~~ — **fixed**, see below.

### Review-pass corrections (same session)

Four issues the review caught after the phase-2 work looked done:

1. **The pharmacy demo org was still unscaled — and it showed on the one screen built for comparison.** With a single daily entry, `periodDays = 1`, so inventory turnover annualised one day of COGS and pinned إدارة المخزون at 100; the admin leaderboard read **صيدلية الشفاء 100.0 vs سوبرماركت 82.0**, contradicting the owner dashboard right after the rescale was supposed to fix exactly that. Rebuilt it the same way (60 days of entries, 28% pharmacy margin, inventory ~68K at cost, plus expenses/waste/counts/payment dates/equity). Now **84.3 vs 82.0** — comparable, and both on 13/13.
2. **`equityOf` re-fetched a branch already in hand.** `withExternalSources` now takes the `Branch` and reads `getOrganization().getEquity()` directly; `BranchRepository` dropped from `BhiService` entirely. Matters because `AdminController` calls `averageAcross` once per org, once per city, and once platform-wide.
3. **`app.seed.enabled=false` in the migration test was inert** — `DataSeeder` guards only on `organizationRepository.count() > 0`, there is no such property. Removed rather than left as a false claim.
4. **Waste and stock-count treated "no data" inconsistently.** A zero-variance count scores 100 (an explicit act with a clean result) while zero waste records stays "غير متاح" (genuinely ambiguous — no waste, or nobody logged it?). That asymmetry is deliberate but was undocumented; the reasoning is now a comment next to the code.

**Self-inflicted regression worth noting:** deleting `equityOf` with a text slice also removed the four indicator methods next to it (`paymentEfficiency`, `debtToEquity`, `wasteRatio`, `stockAccuracy`) — the slice's end-marker no longer matched after an earlier rename, so it ran on to the next method. Caught immediately by the compiler and restored. A reminder that range-based text edits over source are fragile once the file has moved on.

**Tenant isolation re-verified on every new endpoint** (this is a multi-tenant app; new controllers are the usual place IDOR creeps in):

| Request | Result |
|---|---|
| org-1 owner → `/api/bhi?branchId=2` | 403 |
| org-1 owner → `/api/expenses?branchId=2` | 403 |
| org-1 owner → `POST /api/waste` on branch 2 | 403 |
| owner → `/api/admin/bhi-config` | 403 |
| platform admin → `/api/bhi?branchId=2` | 403 (correct — admins hold no branch membership; they read aggregates via `/api/admin/overview`) |
| unauthenticated → `/api/bhi` | 401 |
| org-1 owner → own branch 1 | 200 |

Owner dashboard and `/api/bhi` still return byte-identical scores for the same window (`78.07041105048971`).

## Remaining vision-doc items — functional pass (2026-08-28, same session)

User: "proceed with other items mentioned in the md so the rest of screens and features are working and functional (skip the auth thing for now)". Applied that filter literally — non-screen work was deferred, listed at the end.

### Supplier Portal is no longer a dead end (was the only truly non-functional screen)

`Purchase` gains `supplierResponse` (PENDING/ACCEPTED/REJECTED), `supplierRespondedAt`, `supplierPromisedDate`, `supplierRejectionReason` (**V11**). New `PATCH /api/supplier/orders/{id}/accept|reject`, and a response cell in the portal table with a promised-date picker and a rejection-reason field. Accepting notifies the buying org (SUCCESS), rejecting notifies as WARNING.

**Design choice:** a separate `supplierResponse` column rather than new `Purchase.Status` values. Supplier acceptance is a *commitment to supply*, not a receipt — merging them would have made acceptance update inventory before goods arrived. It also avoids editing V3's already-applied check constraint.

**The security shape changed and needed explicit handling.** `SupplierPortalController.overview` is safe with no tenant guard because every query is scoped by `principal.email()` and nothing is client-supplied. An accept/reject endpoint takes a `purchaseId` *from the client*, which destroys that property. `SupplierPortalService.requireOwnPendingOrder` verifies the purchase's supplier email matches the caller's, and a **nonexistent id returns 403, not 404**, so the endpoint can't be used to probe which ids exist. Verified live:

| Request | Result |
|---|---|
| supplier accepts own SENT order | 200, buyer notified |
| responding twice | 409 |
| responding to a RECEIVED order | 409 |
| owner (not the supplier) responds | 403 |
| nonexistent purchase id | 403 (no existence leak) |

### A3 — the decision card now answers both questions

`Decision` gains `ifIgnoredSummary`, `constraintsSummary`, `confidenceReasons` (**V12**), all derived from values the engine already computed — no new data source. The card shows an amber "لو تجاهلت" line, the constraints actually applied, and bullet reasons behind the confidence number instead of a bare percentage. When nothing constrained the order that is *stated*, because an empty section doesn't prove the check ran.

### B2 — alternatives, respecting the same caps

`DecisionAlternativeBuilder` offers conservative / recommended / extended around the engine's quantity. **Every option passes through the same liquidity cap the primary suggestion does** — offering an option the organization's own policy forbids would be worse than offering none. When the cap collapses options to the same quantity they are deduplicated: three identical choices fake an agency the owner doesn't have.

**Approving an alternative routes through the existing approve/modify path**, so it creates a real `Purchase(SENT)`. Verified in the browser: choosing the conservative option for olive oil created purchase id=19 for 139 units and set the decision to MODIFIED/approvedQuantity=139.

### B5 — goals are honest now

Seven goals grouped under the three strategic pillars, and the five that reach no engine are badged **«لا يؤثّر بعد»**. Deliberately did *not* wire the inert five into the engine — the vision doc and PROGRESS.md both say not to attach numbers to engines that don't exist; doing so would widen the credibility gap, not close it.

### A2 + B3 — "فرص اليوم" replaces hunting across screens

`OpportunityFeedService` merges three signal types into one queue ranked by impact × urgency and **capped at 5** (the doc's own rule — fifty alerts means the app gets closed): open purchase decisions, capital tied up in stagnant stock, and near-expiry goods. Each carries a suggested action, and the stagnant/expiry ones point at the group-order route — the approved revenue path. Verified live: 5 signals, correctly ordered by score (stagnant juice 9,600 > chocolate 4,778 > oil 1,790 > cheese 1,600 > rice 1,004).

**A2 was applied narrowly on purpose.** "توصيات" appears in 9 components, but `Recommendation` is the *legacy rule engine* — a separate feature with its own page. Relabelling those too would have made the UI less coherent, so only the Decision-backed surfaces got the الفرص framing.

### A real engine defect surfaced by the earlier rescale

`SalesEstimator` estimated demand as **a fixed percentage of stock on hand** (15%/day for FAST). That is circular: coverage days come out constant regardless of stock, so the reorder engine could never distinguish an overstocked item from a scarce one. The inventory rescale made it visible — it claimed 2,800 frozen chickens would run out in 1.3 days in a shop taking 3,500 ₪/day.

Replaced with an estimate **anchored to actual COGS** from daily entries, allocated across items by movement class.

**First attempt was wrong and a weak test let it pass.** Allocating by *inventory value* reproduces the circularity in a new form — the quantity cancels out of the coverage formula, giving every item in a movement class identical coverage. The test only used a single item, so it passed. Strengthened it to two items of the same class (one stocked, one scarce), watched it fail, then switched allocation to movement class alone. Coverage now responds to stock while the total still ties to real sales. Decisions went from implausible (1.3 days on 2,800 units) to sensible: overstocked chicken generates nothing, scarce oil and chocolate do.

### Verification

**153/153 backend tests** (115 → 153 this pass), `tsc --noEmit` and `vite build` clean, zero console errors across dashboard / decisions / settings / supplier portal. Full loop exercised in the browser: decision → approve alternative → real PO → supplier queue → accept → buyer notification.

### Deferred, and why

- **§4 onboarding / first-value wizard** — gated behind first login and the ToS gate, i.e. the auth area the user asked to skip. Flagging it explicitly in case that isn't what "the auth thing" meant.
- **CI workflow, code-splitting (780KB bundle), A4 Trust Dictionary** — none are screens or features; excluded by the user's own framing.
- **B6 four-component quality score** — a presentation change over a number that already works; lowest value per effort in this pass.
- **Postgres verification of V11/V12** — Docker still down. `BhiMigrationMatchesEntitiesTest` now also binds V11's four columns, and it provably fails on drift, but that is not the same as applying the migrations.

## UI/UX overhaul — light-first, real icon system, fixed layout shell (2026-08-28)

User asked for a professional pass: light mode as default, no emoji icons, and an end to the
"vibe-coded" tells — naming the scrolling sidebar specifically. Loaded `frontend-design` and
`ui-ux-pro-max`, audited all 22 pages, then rebuilt the system rather than retouching pages.

### What the audit found

| Issue | Evidence |
|---|---|
| Sidebar scrolled away with the page | `.app-shell{min-height:100vh}` + body scroll; no sticky, no own scroll |
| Emoji as icons | **103 occurrences, 46 distinct glyphs, 24 files** |
| Dark default | `getStoredTheme()` returned dark unless explicitly light |
| Almost no focus system | 1 focus rule in 635 lines; **0** `prefers-reduced-motion` |
| Table headers scrolled off | no sticky `thead` anywhere |
| Styling in JSX | 449 inline `style={{}}` — spacing/colour drifted page to page |
| Dead tokens | 5 gradient KPI tile tokens defined, used nowhere |

### Design direction, and where I overrode the tooling

`ui-ux-pro-max` returned the **Data-Dense Dashboard** pattern (correct) with a blue palette and
**Fira Code / Fira Sans** typography. I rejected the typography outright — **Fira Code has no
Arabic coverage**, and this is an Arabic-first RTL product. Kept **IBM Plex Sans Arabic** (already
loaded, technical, corporate) and added IBM Plex Sans for Latin so data columns get **tabular
numerals** and actually align.

I also rejected the default blue. Chose **petrol/deep-teal `#0f5257`**:
- blue-corporate is the generic dashboard look and reads as templated;
- more importantly, blue was already carrying an *info* semantic. Making it the brand too would
  have muddied it. Petrol is structural-only (sidebar, primary action, active state, primary data
  series), which leaves **green and red to mean only opportunity and risk** — the product's two
  core categories.

In dark mode the brand *lightens* to `#3fa9a4` rather than inverting, so the identity survives
the theme switch instead of becoming a different colour.

### The layout fix

```
.app-shell { height: 100dvh; overflow: hidden; }   /* shell never scrolls */
.sidebar   { height: 100%; overflow-y: auto; }     /* nav scrolls itself */
.main-area { height: 100%; overflow-y: auto; }     /* content scrolls itself */
.page-header { position: sticky; top: 0; }         /* screen name stays put */
```

Verified by scrolling deep into the dashboard: sidebar, active nav item, and the top bar all
remain. Table `thead` is sticky too. On mobile the sticky header would have slid under the fixed
menu button (both sit at the RTL start edge), so the button moved to `inset-inline-start` and the
header gained clearance.

### Icon system

New `src/components/Icon.tsx` wraps **lucide-react** (MIT) behind **semantic** names — `risk`,
`opportunity`, `liquidity`, `stagnant` — not shape names. If the glyph for "risk" ever changes it
changes in one file. Uniform 1.5–1.75px stroke, `currentColor` so it follows the theme, and
`aria-hidden` unless the icon carries meaning no adjacent text repeats.

**103 → 0 emoji icons.** The only remaining glyphs are `★☆` in a rating `<select>`, which is
deliberate: SVG cannot go inside `<option>`, and those two are monochrome text symbols that
render consistently.

### Other fixes made along the way

- **KPI/metric cards** were rendering the icon *name as literal text* after the swap — rewrote both
  to take an `IconName`, and replaced the saturated colour blobs with a tinted glyph so the number
  stays the loudest thing on the tile.
- **Decision actions** used three different button styles with icons stacking above labels on two
  of them. Now one primary (اعتماد), two secondary, and تجاهل pushed to the far end as a quiet
  ghost above a divider — one clear primary action per card.
- **Delta arrows** were `▲▼` text glyphs; now icons paired with text, so direction is not conveyed
  by colour alone.
- **Health radar** was painted green regardless of the score — colour implying "good" for a value
  that might be bad. Now brand-neutral; the gauge beside it carries the semantic banding.
- Login mark's blue→purple gradient replaced with the flat brand; dead `--tile-*` gradients deleted;
  modal and drawer scrims tokenised.

### Verification

`tsc --noEmit` and `vite build` clean, zero console errors. Checked in-browser: owner dashboard,
decisions, inventory, admin overview, login, and dark mode. Backend untouched — 153/153 still green.

### Not done in this pass

- The **449 inline `style={{}}`** are still inline. The token layer now governs colour and spacing,
  so drift is contained, but moving them into classes is a separate mechanical pass.
- Only a **light audit of the remaining pages** (pricing, profitability, reports, sales, group
  orders) — they inherit the shell, tokens, and icons, but their internal layouts were not
  individually reworked.
