# TRUST — Inventory page, Daily Entry modal, Health gauge fix

**Date:** 2026-07-02
**Status:** Approved by user

## Context

Handover backlog items #3 and #4 (see `handover.md`), plus a visual bug fix: the
"صحة الأعمال" (Health Score) dashboard card currently reuses the plain
`MetricCard` component instead of the circular gauge shown in the reference
mockup.

Both backend endpoints needed already exist and require no changes:
- `GET /api/items?branchId=` → `ItemDto[]` (includes `movementStatus`)
- `POST /api/items` (`ItemCreateRequest`) → creates an item
- `POST /api/entries/daily` (`DailyEntryRequest`) → upserts a daily entry (unique on branch+date)

`branchId`/`organizationId` stay hardcoded to `1` throughout, consistent with
the existing hardcoded `organizationId=1` in `Dashboard.tsx` (both are known
debt resolved together when real auth lands — backlog item #7).

## Scope

1. Minimal `react-router-dom` routing: `/` (Dashboard) and `/inventory`
   (new InventoryPage). Sidebar's "الرئيسية" and "المخزون" become real links;
   the other 9 items stay inert, unchanged.
2. Inventory page: tabbed table (الكل/سريع/متوسط/بطيء/راكد) filtered
   client-side on `movementStatus`, "+ إضافة صنف" button opening an add-item
   modal.
3. Daily Entry modal, triggered from a new "+ إدخال بيانات اليوم" button in
   the Dashboard topbar.
4. Health Score gauge: replace the plain metric card with an SVG radial
   progress ring.

Out of scope (explicitly, per prior decisions): chatbot widget, any other
sidebar page, backend changes, automated tests (none exist in this project
yet — verification is manual via the running dev servers).

## Components & data flow

### Shared

- `src/components/Modal.tsx` — overlay + panel, closes on backdrop click,
  Escape key, or an X button. Takes `title`, `onClose`, `children`.
- `src/api/client.ts` gains:
  - `fetchItems(branchId): Promise<ItemDto[]>` — `GET /api/items`
  - `createItem(req: ItemCreateRequest): Promise<ItemDto>` — `POST /api/items`
  - `submitDailyEntry(req: DailyEntryRequest): Promise<DailyEntryDto>` — `POST /api/entries/daily`
  - Matching TypeScript interfaces mirroring the backend DTOs exactly
    (`ItemDto`, `ItemCreateRequest`, `DailyEntryRequest`).

### Routing

- `src/main.tsx`: wrap `<App />` in `<BrowserRouter>`.
- `src/App.tsx`: `<Routes>` with `/` → `<Dashboard />`, `/inventory` →
  `<InventoryPage />`.
- `src/components/Sidebar.tsx`: drop the `active` prop; compute the active
  key internally via `useLocation()` (`/` → `home`, `/inventory` →
  `inventory`). Items with a route (`home`, `inventory`) render as
  `<Link>`; the rest render as today (plain `div`, no-op).

### InventoryPage (`src/pages/InventoryPage.tsx`)

- On mount: `fetchItems(1)`, store in state; loading and empty states like
  `Dashboard.tsx`'s existing pattern.
- Tab state (`all | FAST | MEDIUM | SLOW | STAGNANT`), filters the fetched
  list client-side — no refetch on tab change.
- Table columns: الاسم, الفئة الفرعية, سعر التكلفة, سعر البيع, هامش الربح,
  الكمية, القيمة, تاريخ آخر بيع, تاريخ الانتهاء, الحالة (status badge reusing
  `.status-FAST/.status-SLOW/.status-MEDIUM/.status-STAGNANT` from
  `theme.css`).
- Row highlight: if `expiryDate` is within 30 days of today (and not null),
  add a warning row style (reuse `--accent-red`/`--accent-amber` per plan
  §7.3).
- Header has "+ إضافة صنف" button → opens `AddItemModal`.
- `AddItemModal` (`src/components/AddItemModal.tsx`): fields per plan §3.2 —
  name (required), subCategory (optional text), costPrice, salePrice,
  quantity (all required, positive), lastSaleDate, expiryDate (optional
  dates). Live-computed margin % shown below price fields:
  `((salePrice - costPrice) / salePrice) * 100`, hidden until both prices are
  positive numbers. Client-side validation (required + positive) blocks
  submit with inline field errors. On submit: `createItem({branchId: 1, ...})`;
  on success, close modal and prepend/refetch so the new item shows
  immediately; on API error, show a non-blocking inline error banner in the
  modal with the response message (no silent catch).

### Daily Entry modal (`src/components/DailyEntryModal.tsx`)

- Triggered from a new button in `Dashboard.tsx`'s topbar area.
- Fields per plan §3.1: date (defaults to today, editable — supports
  correcting past days since the endpoint upserts on branch+date), total
  sales, total COGS, total profit (auto-computed as sales−COGS, but the
  field stays editable — manual edits stick and are what's actually sent),
  available liquidity, receivables, payables (last three optional, default
  0). Live margin % preview: `(profit / sales) * 100`.
- Client-side validation: sales/COGS/liquidity/receivables/payables must be
  ≥ 0 (matches backend `@PositiveOrZero`).
- On submit: `submitDailyEntry({branchId: 1, entryDate, ...})`; on success,
  close the modal and re-run `fetchDashboard` so the dashboard's numbers
  reflect the new entry immediately; on API error, inline error banner, no
  silent catch.

### Health gauge (`src/components/HealthGauge.tsx`)

- Pure SVG circular progress ring: background track circle +
  foreground arc via `stroke-dasharray`/`stroke-dashoffset` proportional to
  `score/100`. Arc color by band matching plan §4.7: 81–100 green
  (`--accent-green`), 61–80 a lighter/teal green, 41–60 amber
  (`--accent-amber`), 0–40 red (`--accent-red`). Centered text: big score
  number + "من 100" below, and the status label (e.g. "جيد") below the ring,
  matching the mockup's layout.
- Replaces the inline plain-card markup at `Dashboard.tsx:81-86` inside
  `grid-metrics` — same card wrapper/grid slot, new internal content only.

## Error handling

No silent `.catch(() => {})` on the two form submissions or the item/dashboard
fetches that drive page content — errors surface as inline banners. The
existing `Dashboard.tsx` mock-data fallback behavior for the initial
dashboard fetch is unchanged (that catch is intentional/documented existing
behavior, not new code).

## Verification (manual — no test framework present in this project)

1. `/inventory` loads the 6 seeded items, tabs filter correctly, an item
   with `expiryDate` < 30 days out is visually highlighted.
2. Add-item modal: submit a new item, confirm it appears in the table with
   correct computed margin and lands in the correct tab.
3. Daily Entry modal: submit today's entry, confirm dashboard numbers
   (sales/profit/margin/liquidity cards, sales chart) update after close.
   Resubmit same day to confirm upsert doesn't error.
4. Health gauge: visually matches the mockup's ring style at the current
   seeded score; sanity-check the arc math by temporarily eyeballing it
   against the plain `{score}/100` text still present inside it.
5. Sidebar: clicking "الرئيسية"/"المخزون" navigates via router (URL changes,
   no full page reload); other 9 items remain inert as before.

## Explicitly not doing

- No backend changes (both endpoints already match).
- No automated tests (none exist in this project; would be scope creep).
- No changes to the other 9 sidebar pages, auth, scheduler, or admin panel —
  separate backlog items.
