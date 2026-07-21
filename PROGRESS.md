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
