# TRUST — Vision Doc → MVP Analysis

Source: `ملخص ترست.docx` (a ~10.5k-line founder-partner strategy dialogue, read in full).
Purpose: extract what actually reshapes the MVP, mapped against what's **already built** (see `PROGRESS.md`).

> **Read this as a delta, not a philosophy recap.** Each item is: *doc principle → what exists today → the gap → a concrete change you could put on a backlog.*

---

## 0. The one-line thesis (and why it matters for the build)

The doc iterates dozens of reframings but converges on a single, stable idea:

> **ترست لا يدير الأعمال، بل يدير جودة القرارات التنفيذية التي تقود الأعمال.**
> *TRUST doesn't manage the business; it manages the quality of the executive decisions that drive it.*

And its final, deeper pivot:

> **ترست يدير دورة حياة الفرصة التجارية** — *TRUST manages the lifecycle of the commercial **opportunity**.*
> A decision is only **one stage** in a bigger cycle:
> `البيانات → المعلومات → المعرفة → الفرص والمخاطر → القرار → التنفيذ → الأثر → التعلم`
> (Data → Information → Knowledge → Opportunity/Risk → Decision → Execution → Impact → Learning)

**Good news:** the current MVP already implements the *decision* half of this cycle end-to-end (the `Decision` entity, the 5-element decision card, confidence, approve → real PO → receive → inventory update → outcome → quality score). The doc's "big" moves are therefore mostly **reframes and thin layers on top of what exists**, not a rebuild. The analysis below keeps that in view.

---

## 1. What the doc and the current build already AGREE on (validation, no action needed)

These are confirmations that the existing direction is right — worth stating so we don't "fix" what's working:

| Doc principle | Already in the build |
|---|---|
| Decision is the unit of value; every screen is an interface to decisions | `Decision` entity + dedicated "قرارات الشراء" page |
| Explainable AI, never a black box (reason + confidence + impact + action) | Decision card shows reason, confidence %, financial impact, Approve/Modify/Defer/Dismiss |
| Human owns the decision — TRUST discovers/analyzes/recommends but never auto-executes | Approve/Modify/Defer/Dismiss is manual; no auto-purchase |
| Close the loop: measure impact after execution | `PATCH /purchases/{id}/receive` → writes `actualOutcome`, nudges supplier rating, updates quality score |
| Per-tenant configurable policies instead of hardcoded constants | `Policy` entity (liquidity ratio, min supplier rating) |
| Tenant-ranked business goals reweight recommendations | `Goal` entity (2 of 7 wired) |
| Don't build sales/accounting/POS/ERP; only purchasing + inventory + suppliers | Exactly the current scope |
| Supplier is a partner, scored, not always-cheapest | `Supplier.rating` + policy-driven supplier check with confidence penalty |

**Takeaway:** the PM notes that drove the current build clearly came from *this same thinking*. The MVP is a faithful vertical slice of the doc's decision half. The value now is in the pieces the doc emphasizes that the build hasn't reached yet.

---

## 2. TIER A — Cheap reframes / relabels (hours, very high leverage)

These change how the product *reads* to a shopkeeper without new engines. The doc argues these ARE the product ("المستخدم لا يبحث عن المعلومات، بل عن الإجراء التالي").

### A1. Make the home screen a "المكتب التنفيذي / Executive Action Center", capped at 5
- **Doc:** On open, the owner must NOT see "15 charts / 40 KPIs" but *"هذه أهم 5 قرارات تحتاج اهتمامك اليوم، مرتبة حسب أثرها المتوقع"* — top 5 items ranked by expected impact. Hard rule: **≤ 5 items on the home screen** ("50 alerts → user closes the app").
- **Now:** Dashboard is metrics-first (health gauge, radar); decisions live on a separate page you must navigate to.
- **Gap:** The first thing the user sees is data, not "what do I do today?"
- **Change:** Surface the top 5 open `Decision`s (ranked by `financialImpact` × urgency) as cards on the Dashboard home. Push the gauges/radar below the fold or to a "الأداء / Performance" tab. No new backend — you already have open decisions and financial impact.

### A2. Relabel التوصيات → الفرص (Recommendations → Opportunities) in the UI
- **Doc:** Strongest marketing idea in the doc — *"التاجر لا يشتري توصية، بل يستثمر فرصة"* (a merchant doesn't buy a recommendation; he invests in an opportunity). Home screen becomes **"فرص اليوم / Today's Opportunities"** with color codes: 🔴 risk, 🟠 improvement, 🟢 opportunity.
- **Now:** UI says "توصيات" / "قرارات".
- **Gap:** Framing is system-centric ("here's a recommendation"), not owner-centric ("here's an opportunity worth money").
- **Change:** UI-label change + a color-coded type badge on each card. **⚠️ Decision to make, not to just do:** going all-in on الفرص terminology at the *data* layer would mean renaming `RecommendationEngineService` and DB columns — a real migration. Recommend: relabel at the **presentation layer only** for now; keep backend names.

### A3. Enrich the decision card with the two lines that build trust
- **Doc (Constitution Art. 3 + 12-part card spec):** Every recommendation must answer **two** questions: *"ماذا سيحدث إذا نفذتها؟ ماذا سيحدث إذا تجاهلتها؟"* (what if you execute vs. what if you ignore) — plus a **constraints line** (*"القيود"* — the recommendation must respect real liquidity/space/obligations, not be theoretically ideal), and **confidence with reasons** (not a bare %).
- **Now:** Card shows reason + a single confidence % + financial impact (the "execute" side only).
- **Gap:** No explicit "cost of ignoring," no constraints line, confidence is a bare number.
- **Change:** Add to the existing card: (1) a "لو تجاهلت / If ignored" impact line, (2) a "قيود / Constraints considered" line (liquidity, space — data already used by the engine, just surface it), (3) 2–3 bullet reasons behind the confidence score. All derivable from data the engine already computes.

### A4. Write the قاموس ترست (Trust Dictionary) as a repo doc
- **Doc:** Each term gets exactly one definition shared by dev/AI/design/marketing: البيانات، المعلومات، المعرفة، الخبرة، الفرصة، المخاطرة، التوصية، القرار، الأثر.
- **Now:** No shared glossary; UI labels are ad hoc.
- **Change:** Add a short `TRUST-DICTIONARY.md` and use these exact terms consistently in UI + code comments. Nearly free; pays off in every future feature and every UI label.

---

## 3. TIER B — Real builds (days–weeks), ranked by leverage

### B1. Monthly Impact Ledger — "دفتر الإنجازات / تقرير الأثر الشهري"  ← highest-leverage build
- **Doc:** The single strongest anti-churn / trust tool. Not a usage report — an *impact* report: *"وفرنا عليك 3,850 شيكل… منعنا 1,200 شيكل توالف… سرّعنا دوران رأس المال من 48 إلى 36 يومًا"*. "العميل يرى العائد، لا الفاتورة" (the customer sees the return, not the bill). Also framed as 5 morning result-cards: 💰 saved / 📈 profit up / 📦 inventory rescued / ⚡ turnover faster / 🎯 next opportunity.
- **Now:** `GET /decisions/quality-score` exists; `actualOutcome` is recorded per received order. But there's no aggregation into a "here's what TRUST did for you this month" view.
- **Gap:** The value TRUST creates is invisible in aggregate — exactly the thing that makes an owner keep using it.
- **Change:** Aggregate the outcomes you already record (`Decision.financialImpact`, `actualOutcome`, received-order savings, stagnant-stock reductions) into a monthly summary endpoint + a simple screen. This is mostly a read-model over existing data.
- **Why it matters even with no billing:** the user's standing instruction is *prove value → get users → group orders make money*. An impact ledger is the mechanism that proves value. It's on-strategy precisely *because* there's no subscription to justify.

### B2. Alternatives / scenarios (Option A / B / C)
- **Doc (repeated ~5×, treated as non-negotiable):** *"لا توجد توصية دون بدائل"* — never one option. Card should show e.g. (A) buy 50 now, (B) buy 30, (C) delay 2 days — each with pros + risk + expected impact — then "we recommend B because best balance." Preserves the owner's sense of control and is core to trust.
- **Now:** The engine emits a single `suggestedQuantity`.
- **Gap:** The card is "take it or leave it," which the doc argues erodes trust.
- **Change:** Have `PurchaseDecisionEngineService` compute 2–3 candidate quantities/timings and attach them to the `Decision` (new `alternatives` field), with the recommended one flagged. Medium effort; the underlying math (coverage vs lead time + safety) already exists — this is generating a small grid around the optimum, not a new engine.

### B3. Opportunity/Risk discovery + ranking layer (the "opportunity pivot", done thin)
- **Doc:** TRUST's "brain" is 4 engines: **Discovery → Evaluation → Decision → Learning**. Discovery surfaces anything worth attention (stockout risk, stagnation, negotiation opportunity, margin lift); Evaluation ranks by expected impact × urgency × success-probability × ease.
- **Now:** Decisions are generated for reorder; there's a stagnant-items view; but there's no unified "discovery feed" that ranks *all* opportunity/risk types together on one queue.
- **Gap:** No single ranked queue across types; each lives on its own page.
- **Change (kept thin — this is NOT a rearchitecture):** add a ranking service that pulls existing signals (open reorder decisions, stagnant items, expiring stock, low-rating suppliers) into one scored list feeding the Tier-A1 home screen. Frame internally as "pilot the reframe on what exists." Full multi-engine build is deferred (see §5).

### B4. The 5 decision *categories* / a second decision type
- **Doc:** All decisions fall into 5 categories: **الفرص (opportunity) / المخاطر (risk) / التحسين (improvement) / المتابعة (follow-up) / التعلم (learning)**. And the roadmap: after purchasing, add pricing, then supplier-switch, clearance, etc.
- **Now:** `Decision.type` has only `PURCHASE_ORDER`. The 5 categories aren't modeled.
- **Gap:** The system can only "discover" one kind of thing.
- **Change:** (a) add a `category` badge to decisions now (cheap, supports A2's color codes); (b) the natural next *type* is **pricing (ADJUST_PRICE)** — `RecommendationEngineService` already has the underlying logic; the work is porting it into the `Decision` explainability format. Do this only when B1–B3 land.

### B5. Collapse 7 Goals → 3 strategic pillars
- **Doc:** The single most important simplification decision — start from **3 goals**, not 10: (1) تعظيم الربحية / maximize profitability, (2) رفع كفاءة رأس المال العامل / working-capital efficiency, (3) رفع الكفاءة التشغيلية / operational efficiency. "المنصات العظيمة تتميز بالبساطة."
- **Now:** 7 goals exist; only 2 affect any engine; the other 5 are stored-but-inert.
- **Gap:** 5 goals are dead UI that imply capability that doesn't exist (honesty/clarity problem).
- **Change:** Regroup the goals UI under the 3 pillars, and either hide or clearly mark the inert ones as "coming soon" rather than presenting 7 equal levers. Low effort, removes a credibility gap. (Do **not** wire numbers to engines that don't exist — the doc and PROGRESS.md agree on this.)

### B6. 4-dimension impact + 4-component quality score
- **Doc (TEIF + DQS):** Impact is 4 dimensions — **مالي / تشغيلي / مخاطر / ثقة** (financial / operational / risk / trust). Decision Quality Score is 4 components — **data / analysis / execution / result** — shown as e.g. "جودة القرار: 88/100" with a breakdown, never an opaque number.
- **Now:** Quality score is a single metric (% of received orders with zero discrepancy).
- **Gap:** Impact and quality are one-dimensional.
- **Change:** Extend the score into the 4 named components with a breakdown display. Medium effort; partly a presentation change over data you have + partly new signals. Lower priority than B1–B3.

---

## 4. Onboarding / first-value journey (worth its own line)

- **Doc ("رحلة القيمة الأولى"):** Within the **first session**, the owner must get a first useful discovery. Flow: **"عرّف ترست على تجارتك"** (minimal input: store size, ~#SKUs, #suppliers, cash-vs-credit) → load whatever data exists (past purchases file / inventory list / manual) → then **do NOT show a dashboard**, show *"اكتشفنا 5 فرص لتحسين تجارتك"* (TRUST's first decision report). Explicit rule: **lack of sales data must not block entry** — Phase 1 is purchases/inventory/suppliers only, and messaging must be *"لن نطلب منك بيانات المبيعات… احتفظ بخصوصية بياناتك"* (we won't ask for your sales; keep your data private) because Palestinian shop owners fear sharing data.
- **Now:** Demo data is seeded; admin creates orgs with a temp password; there's a ToS gate — but no guided "introduce your business → here are your first 5 opportunities" first-run.
- **Change:** A short first-run wizard (business profile + one CSV import — the CSV import already exists) that ends on the Executive Action Center showing the first ranked opportunities. This is what produces the doc's "aha moment" (*"هذا النظام رأى شيئًا لم أكن أراه"*). Pairs naturally with A1.

---

## 5. What the doc tells us to DEFER (use its own discipline as the filter)

The doc argues *against* over-building at least as often as it dreams big. Cite this to justify **not** doing things:

- **Only 3 engines/"minds" in Phase 1:** عقل الشراء + عقل المخزون + عقل المورد (purchasing + inventory + supplier). Everything else waits.
- **Risk-Driven MVP:** build to prove **5 hypotheses**, in order: (1) does the owner *trust* the recommendations? (2) do they create *felt* economic impact? (3) is data easy to get? (4) does the user return daily? (5) will they pay for the value. "First success is trust, not algorithm accuracy."
- **Success metric = decisions executed & impact felt, NOT user count.**
- **Explicitly out of scope (doc agrees):** sales management, accounting, POS, ERP. Position as *"طبقة الذكاء التنفيذي فوق الأنظمة الحالية"* — an intelligence layer **above** existing systems; integrate, don't replace.
- **Defer:** the full multi-engine "4-layer brain," the 7-platform "TRUST 2030" suite, the market-intelligence network, the Economic Graph, scenario libraries. All real, all later.

---

## 6. Monetization: mostly out of scope — with one sharp on-strategy exception

- **Out of scope (per your standing instruction in PROGRESS.md):** the doc is saturated with subscription tiers (basic/pro/advanced), 5 revenue layers, paid supplier promotions, market-report sales. Your governing rule is **no monetization/billing**; revenue comes from **group orders**. So all subscription/pricing/billing content in the doc = **do not build.**
- **The exception that IS on-strategy — elevate it:** the doc's **Trust Connect / محرك التعاون (Collaboration Engine) / الشراء الجماعي (group buying)** and its "turn a risk into an opportunity" move — e.g. an item near expiry becomes a **group-buy, a discount, or a transfer to another store** (*"فيتحول الخطر إلى فرصة"*). This is *exactly* the approved revenue path (group orders through the system), and the build already has a group-purchasing flow (collect → negotiate → distribute) + a clearance-matching view. It's easy to miss because it's buried among the out-of-scope subscription talk.
- **Change:** Treat group-buying / opportunity-matching as the **one business-model area to deepen**: wire the discovery layer (B3) so that a detected risk (near-expiry, overstock, stagnant capital) can generate a **group-order opportunity card** that routes into the existing group-purchasing flow. That's revenue-generating *and* on-vision.

---

## 7. Recommended sequence (choose-your-move)

Nothing here is built yet — this is analysis. If we proceed, the highest value-per-effort order is:

1. **A1 + A3 + A2** — Executive Action Center home (top-5 ranked opportunity cards) with the enriched card (if-ignored line, constraints, confidence reasons) and opportunity relabel/color-codes. *(~1–2 focused sessions, presentation-layer, no migration.)*
2. **B1** — Monthly Impact Ledger over data you already record. *(Highest strategic leverage: it's how you "prove value → get users.")*
3. **B3 (thin) + §6** — Unified opportunity/risk ranking feed, wired so risk items can spawn group-order opportunities. *(Ties the reframe to the approved revenue path.)*
4. **B2** — Option A/B/C alternatives on the purchase card.
5. **B5 + A4** — Collapse goals to 3 pillars; write the Trust Dictionary. *(Cheap cleanups.)*
6. Later: **B4** (pricing decision type / 5 categories), **B6** (4-dimension impact), full onboarding wizard (§4).

**Deliberately deferred (per §5):** multi-engine brain, 7-platform suite, market-intelligence network, subscription/billing (§6).

---

*Analysis produced from a full read of `ملخص ترست.docx`; current-state claims cross-checked against `PROGRESS.md`.*
