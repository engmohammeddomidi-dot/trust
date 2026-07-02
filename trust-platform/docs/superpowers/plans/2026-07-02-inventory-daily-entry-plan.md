# Inventory Page, Daily Entry Modal, Health Gauge — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Inventory page, Daily Entry data-entry flow, and the Health Score gauge fully functional end-to-end against the real backend.

**Architecture:** Fix a pre-existing backend serialization bug that blocks Daily Entry submission, then add minimal `react-router-dom` routing, a reusable `Modal`, two new forms (`AddItemModal`, `DailyEntryModal`), a new `InventoryPage`, and a `HealthGauge` SVG-based (Recharts `Pie`) component replacing the plain metric card.

**Tech Stack:** Spring Boot 3.3.2 / Java 21 (backend, `trust-backend`), React 18 + TypeScript + Vite + react-router-dom 6 + Recharts + axios (frontend, `trust-frontend`). No test framework in either project.

## Global Constraints

- `branchId`/`organizationId` stay hardcoded to `1` everywhere (matches existing `Dashboard.tsx` debt; real auth is a separate backlog item).
- No automated tests exist in either project — verification is manual via `curl` (backend) and the running Vite dev server (frontend). Do not introduce a test framework as part of this plan.
- Neither `C:\Users\m.domidi_freightos\Desktop\trust-platform` nor `trust-platform\trust-platform` is a git repository — **skip all `git commit` steps**. Mark each task's final step "Done" instead of committing.
- Follow existing code conventions exactly: CSS variables from `theme.css` only (no new hex colors), Recharts for any chart/gauge (matches `DonutBreakdown.tsx`'s pattern), inline `style={{}}` for one-off layout per existing components, dedicated `.css` classes in `theme.css` for reusable patterns (modal, tabs, forms).
- Backend must be restarted (no devtools/hot-reload) after any Java change: kill the running `mvn spring-boot:run` process and relaunch with `JAVA_HOME` pointed at `C:\Users\m.domidi_freightos\.jdks\temurin-21.0.7` (the system default `JAVA_HOME` is JDK 17, which fails the build — this bit us once already this session).

---

### Task 1: Fix `DailyEntryController` to return a DTO instead of a raw JPA entity

**Problem:** `POST /api/entries/daily` currently returns the raw `DailyEntry` entity. Jackson tries to serialize its lazy `branch → organization` Hibernate proxy chain and throws `InvalidDefinitionException`, which Spring's `ExceptionTranslationFilter` surfaces as an opaque `403` (verified live: `curl -X POST http://localhost:8080/api/entries/daily -d '{...}'` → `403`, backend log shows `No serializer found for class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor ... DailyEntry["branch"]->Branch["organization"]`). This is the same class of bug `ItemController` already avoids by returning `ItemDto`.

**Files:**
- Create: `trust-backend/src/main/java/com/trust/web/dto/DailyEntryDto.java`
- Modify: `trust-backend/src/main/java/com/trust/web/DailyEntryController.java`

**Interfaces:**
- Produces: `DailyEntryDto` record with fields `id, branchId, entryDate, totalSales, totalCogs, totalProfit, marginPercent, availableLiquidity, receivables, payables` — this is the exact JSON shape the frontend's `submitDailyEntry()` (Task 4) expects back.

- [ ] **Step 1: Create the DTO**

```java
package com.trust.web.dto;

import com.trust.domain.DailyEntry;

import java.time.LocalDate;

public record DailyEntryDto(
        Long id,
        Long branchId,
        LocalDate entryDate,
        double totalSales,
        double totalCogs,
        double totalProfit,
        double marginPercent,
        double availableLiquidity,
        double receivables,
        double payables
) {
    public static DailyEntryDto from(DailyEntry entry) {
        return new DailyEntryDto(
                entry.getId(),
                entry.getBranch().getId(),
                entry.getEntryDate(),
                entry.getTotalSales(),
                entry.getTotalCogs(),
                entry.getTotalProfit(),
                entry.getMarginPercent(),
                entry.getAvailableLiquidity(),
                entry.getReceivables(),
                entry.getPayables()
        );
    }
}
```

Note: `entry.getBranch().getId()` only reads the FK id off the (possibly lazy) proxy — this never triggers a DB hit or touches `Branch.organization`, so it can't retrigger the same serialization failure.

- [ ] **Step 2: Update the controller to return the DTO**

Replace the full contents of `DailyEntryController.java`:

```java
package com.trust.web;

import com.trust.domain.DailyEntry;
import com.trust.service.DailyEntryService;
import com.trust.web.dto.DailyEntryDto;
import com.trust.web.dto.DailyEntryRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entries/daily")
@CrossOrigin(origins = "*")
public class DailyEntryController {

    private final DailyEntryService dailyEntryService;

    public DailyEntryController(DailyEntryService dailyEntryService) {
        this.dailyEntryService = dailyEntryService;
    }

    @PostMapping
    public DailyEntryDto upsert(@Valid @RequestBody DailyEntryRequest request) {
        DailyEntry entry = dailyEntryService.upsert(request);
        return DailyEntryDto.from(entry);
    }
}
```

- [ ] **Step 3: Restart the backend**

Find and stop the running Maven process (Windows PowerShell), then relaunch with the correct JDK:

```powershell
Get-Process java | Where-Object { $_.Path -like "*temurin-21*" -or $_.MainWindowTitle -like "*trust*" } | Stop-Process -Force
```

If that doesn't find it, find the PID bound to port 8080 and stop it:

```powershell
$p = (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue).OwningProcess
if ($p) { Stop-Process -Id $p -Force }
```

Then relaunch:

```powershell
Set-Location "C:\Users\m.domidi_freightos\Desktop\trust-platform\trust-platform\trust-backend"
$env:JAVA_HOME = "C:\Users\m.domidi_freightos\.jdks\temurin-21.0.7"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
& "C:\Program Files\apache-maven-3.8.5\bin\mvn.cmd" spring-boot:run *> "$env:TEMP\trust-backend.log"
```

Run this as a background task. Wait for `Started TrustApplication` to appear in `$env:TEMP\trust-backend.log` before proceeding (poll every few seconds; do not use long blocking sleeps).

- [ ] **Step 4: Verify the fix**

```bash
curl -s -w "\nHTTP %{http_code}\n" -X POST http://localhost:8080/api/entries/daily \
  -H "Content-Type: application/json" \
  -d '{"branchId":1,"entryDate":"2026-07-01","totalSales":1000,"totalCogs":600,"availableLiquidity":5000,"receivables":100,"payables":200}'
```

Expected: `HTTP 200` with a JSON body like:
```json
{"id":8,"branchId":1,"entryDate":"2026-07-01","totalSales":1000.0,"totalCogs":600.0,"totalProfit":400.0,"marginPercent":40.0,"availableLiquidity":5000.0,"receivables":100.0,"payables":200.0}
```

Re-run the same curl command a second time to confirm the upsert path still works (same `id` returned, no duplicate row, no error).

- [ ] **Step 5: Done** (no git repo — nothing to commit)

---

### Task 2: Add API client functions for items and daily entries

**Files:**
- Modify: `trust-frontend/src/api/client.ts`

**Interfaces:**
- Consumes: existing `apiClient` (axios instance), existing `ItemDto` interface (already defined in this file — do not redefine it).
- Produces: `ItemCreateRequest`, `DailyEntryRequest`, `DailyEntryDto` interfaces; `fetchItems(branchId)`, `createItem(req)`, `submitDailyEntry(req)` functions — these exact names/signatures are what Tasks 3, 5, and 6 import.

- [ ] **Step 1: Append to `client.ts`**

Add after the existing `fetchDashboard` function at the end of the file:

```ts
export interface ItemCreateRequest {
  branchId: number;
  name: string;
  subCategory?: string;
  costPrice: number;
  salePrice: number;
  quantity: number;
  lastSaleDate?: string;
  expiryDate?: string;
}

export async function fetchItems(branchId: number): Promise<ItemDto[]> {
  const { data } = await apiClient.get<ItemDto[]>('/items', { params: { branchId } });
  return data;
}

export async function createItem(req: ItemCreateRequest): Promise<ItemDto> {
  const { data } = await apiClient.post<ItemDto>('/items', req);
  return data;
}

export interface DailyEntryRequest {
  branchId: number;
  entryDate: string;
  totalSales: number;
  totalCogs: number;
  totalProfit?: number | null;
  availableLiquidity: number;
  receivables: number;
  payables: number;
}

export interface DailyEntryDto {
  id: number;
  branchId: number;
  entryDate: string;
  totalSales: number;
  totalCogs: number;
  totalProfit: number;
  marginPercent: number;
  availableLiquidity: number;
  receivables: number;
  payables: number;
}

export async function submitDailyEntry(req: DailyEntryRequest): Promise<DailyEntryDto> {
  const { data } = await apiClient.post<DailyEntryDto>('/entries/daily', req);
  return data;
}
```

- [ ] **Step 2: Verify it compiles**

```bash
cd "C:/Users/m.domidi_freightos/Desktop/trust-platform/trust-platform/trust-frontend"
npx tsc -b --noEmit
```

Expected: no errors (existing `tsconfig.json` has `noUnusedLocals: false`, so unused exports at this stage are fine).

- [ ] **Step 3: Done**

---

### Task 3: Reusable `Modal` component and shared CSS

**Files:**
- Create: `trust-frontend/src/components/Modal.tsx`
- Modify: `trust-frontend/src/styles/theme.css`

**Interfaces:**
- Produces: `Modal` component with props `{ title: string; onClose: () => void; children: React.ReactNode }` — this exact prop shape is what `AddItemModal` (Task 5) and `DailyEntryModal` (Task 6) wrap their content in.

- [ ] **Step 1: Add modal, tab, and form CSS to `theme.css`**

Append to the end of `trust-frontend/src/styles/theme.css`:

```css
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(5, 9, 18, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 20px;
}

.modal-panel {
  background: var(--bg-panel);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 22px;
  width: 100%;
  max-width: 480px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.modal-title {
  font-size: 16px;
  font-weight: 700;
}

.modal-close {
  background: none;
  border: none;
  color: var(--text-secondary);
  font-size: 18px;
  cursor: pointer;
  line-height: 1;
}

.form-group {
  margin-bottom: 14px;
}

.form-group label {
  display: block;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.form-group input {
  width: 100%;
  background: var(--bg-panel-alt);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  padding: 9px 12px;
  color: var(--text-primary);
  font-size: 14px;
  font-family: var(--font-main);
}

.form-group input:focus {
  outline: none;
  border-color: var(--accent-blue);
}

.form-error {
  font-size: 11px;
  color: var(--accent-red);
  margin-top: 4px;
}

.form-banner-error {
  background: var(--accent-red-bg);
  color: var(--accent-red);
  padding: 10px 14px;
  border-radius: var(--radius-md);
  font-size: 13px;
  margin-bottom: 14px;
}

.form-live-margin {
  font-size: 13px;
  color: var(--accent-green);
  margin-bottom: 14px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

.btn-secondary {
  background: var(--bg-panel-alt);
  color: var(--text-primary);
  border: 1px solid var(--border-subtle);
  padding: 10px 18px;
  border-radius: var(--radius-md);
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.tab {
  background: var(--bg-panel);
  border: 1px solid var(--border-subtle);
  color: var(--text-secondary);
  padding: 8px 16px;
  border-radius: var(--radius-md);
  font-size: 13px;
  cursor: pointer;
}

.tab.active {
  background: var(--accent-blue-bg);
  color: var(--accent-blue);
  border-color: var(--accent-blue);
  font-weight: 600;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
}

tr.row-warning td {
  background: var(--accent-red-bg);
}
```

- [ ] **Step 2: Create `Modal.tsx`**

```tsx
import { useEffect } from 'react';
import type { ReactNode } from 'react';

export function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [onClose]);

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-title">{title}</div>
          <button className="modal-close" onClick={onClose} aria-label="إغلاق">✕</button>
        </div>
        {children}
      </div>
    </div>
  );
}
```

- [ ] **Step 3: Verify it compiles**

```bash
cd "C:/Users/m.domidi_freightos/Desktop/trust-platform/trust-platform/trust-frontend"
npx tsc -b --noEmit
```

Expected: no errors. `Modal` isn't imported anywhere yet, but `noUnusedLocals: false` means this is fine.

- [ ] **Step 4: Done**

---

### Task 4: `HealthGauge` component and Dashboard wiring

**Files:**
- Create: `trust-frontend/src/components/HealthGauge.tsx`
- Modify: `trust-frontend/src/pages/Dashboard.tsx:81-86`

**Interfaces:**
- Consumes: nothing new (pure presentational component).
- Produces: `HealthGauge` component with props `{ score: number; label: string }`.

- [ ] **Step 1: Create `HealthGauge.tsx`**

```tsx
import { PieChart, Pie, Cell, ResponsiveContainer } from 'recharts';

function bandColor(score: number): string {
  if (score >= 61) return 'var(--accent-green)';
  if (score >= 41) return 'var(--accent-amber)';
  return 'var(--accent-red)';
}

export function HealthGauge({ score, label }: { score: number; label: string }) {
  const pct = Math.max(0, Math.min(100, score));
  const color = bandColor(pct);
  const data = [{ value: pct }, { value: 100 - pct }];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <div style={{ width: 90, height: 90, position: 'relative' }}>
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={data}
              dataKey="value"
              startAngle={90}
              endAngle={-270}
              innerRadius={32}
              outerRadius={44}
              stroke="none"
              isAnimationActive={false}
            >
              <Cell fill={color} />
              <Cell fill="var(--border-subtle)" />
            </Pie>
          </PieChart>
        </ResponsiveContainer>
        <div
          style={{
            position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column',
            alignItems: 'center', justifyContent: 'center', pointerEvents: 'none',
          }}
        >
          <div style={{ fontSize: 22, fontWeight: 700, lineHeight: 1 }}>{Math.round(pct)}</div>
          <div style={{ fontSize: 10, color: 'var(--text-secondary)' }}>من 100</div>
        </div>
      </div>
      <div className="delta up" style={{ marginTop: 6 }}>{label}</div>
    </div>
  );
}
```

- [ ] **Step 2: Wire it into `Dashboard.tsx`**

Replace lines 81-86 of `trust-frontend/src/pages/Dashboard.tsx`:

```tsx
          <div className="card metric-card">
            <div className="icon" style={{ background: 'var(--accent-green-bg)' }}>📶</div>
            <div className="label">صحة الأعمال</div>
            <div className="value" style={{ fontSize: 30 }}>{Math.round(data.healthScore.totalScore)}<span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>/100</span></div>
            <div className="delta up">{data.healthScore.label}</div>
          </div>
```

with:

```tsx
          <div className="card metric-card">
            <div className="icon" style={{ background: 'var(--accent-green-bg)' }}>📶</div>
            <div className="label">صحة الأعمال</div>
            <HealthGauge score={data.healthScore.totalScore} label={data.healthScore.label} />
          </div>
```

Add the import near the top of `Dashboard.tsx` (after the `MetricCard` import):

```tsx
import { HealthGauge } from '../components/HealthGauge';
```

- [ ] **Step 3: Verify visually**

Start (or confirm already running) the frontend dev server, then check the dashboard in a browser at `http://localhost:5173`. Confirm: the "صحة الأعمال" card shows a circular ring (not plain text), the ring's colored arc proportion roughly matches the score (e.g. a score of 64 shows roughly 64% of the ring filled, colored amber since 41-60... wait 64 is >=61 so green), and the score number + "من 100" + status label render inside/below the ring without visual overflow or clipping in the card.

- [ ] **Step 4: Done**

---

### Task 5: `InventoryPage` with tabs, table, and `AddItemModal`

**Files:**
- Create: `trust-frontend/src/pages/InventoryPage.tsx`
- Create: `trust-frontend/src/components/AddItemModal.tsx`

**Interfaces:**
- Consumes: `fetchItems`, `createItem`, `ItemCreateRequest`, `ItemDto` from `../api/client` (Task 2); `Modal` from `../components/Modal` (Task 3); `Sidebar` from `../components/Sidebar` (existing, will be updated in Task 7 but its current props still work).
- Produces: `InventoryPage` component (no props, used as a route element in Task 7); `AddItemModal` component with props `{ onClose: () => void; onCreated: (item: ItemDto) => void }`.

- [ ] **Step 1: Create `AddItemModal.tsx`**

```tsx
import { useState, type FormEvent } from 'react';
import { Modal } from './Modal';
import { createItem, type ItemDto } from '../api/client';

interface FormState {
  name: string;
  subCategory: string;
  costPrice: string;
  salePrice: string;
  quantity: string;
  lastSaleDate: string;
  expiryDate: string;
}

const initialForm: FormState = {
  name: '', subCategory: '', costPrice: '', salePrice: '', quantity: '', lastSaleDate: '', expiryDate: '',
};

export function AddItemModal({ onClose, onCreated }: { onClose: () => void; onCreated: (item: ItemDto) => void }) {
  const [form, setForm] = useState<FormState>(initialForm);
  const [errors, setErrors] = useState<Partial<Record<keyof FormState, string>>>({});
  const [apiError, setApiError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const cost = parseFloat(form.costPrice);
  const sale = parseFloat(form.salePrice);
  const marginPercent = sale > 0 && !isNaN(cost) ? ((sale - cost) / sale) * 100 : null;

  function update<K extends keyof FormState>(key: K, value: string) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  function validate(): boolean {
    const next: Partial<Record<keyof FormState, string>> = {};
    if (!form.name.trim()) next.name = 'اسم الصنف مطلوب';
    if (!(parseFloat(form.costPrice) > 0)) next.costPrice = 'سعر التكلفة يجب أن يكون أكبر من صفر';
    if (!(parseFloat(form.salePrice) > 0)) next.salePrice = 'سعر البيع يجب أن يكون أكبر من صفر';
    if (!(parseFloat(form.quantity) > 0)) next.quantity = 'الكمية يجب أن تكون أكبر من صفر';
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setApiError(null);
    if (!validate()) return;
    setSubmitting(true);
    try {
      const created = await createItem({
        branchId: 1,
        name: form.name.trim(),
        subCategory: form.subCategory.trim() || undefined,
        costPrice: parseFloat(form.costPrice),
        salePrice: parseFloat(form.salePrice),
        quantity: parseFloat(form.quantity),
        lastSaleDate: form.lastSaleDate || undefined,
        expiryDate: form.expiryDate || undefined,
      });
      onCreated(created);
      onClose();
    } catch (err) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setApiError(message || 'تعذّر حفظ الصنف. حاول مرة أخرى.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title="إضافة صنف" onClose={onClose}>
      <form onSubmit={handleSubmit}>
        {apiError && <div className="form-banner-error">{apiError}</div>}

        <div className="form-group">
          <label>اسم الصنف</label>
          <input value={form.name} onChange={(e) => update('name', e.target.value)} />
          {errors.name && <div className="form-error">{errors.name}</div>}
        </div>

        <div className="form-group">
          <label>الفئة الفرعية (اختياري)</label>
          <input value={form.subCategory} onChange={(e) => update('subCategory', e.target.value)} />
        </div>

        <div className="form-group">
          <label>سعر التكلفة</label>
          <input type="number" step="0.01" value={form.costPrice} onChange={(e) => update('costPrice', e.target.value)} />
          {errors.costPrice && <div className="form-error">{errors.costPrice}</div>}
        </div>

        <div className="form-group">
          <label>سعر البيع</label>
          <input type="number" step="0.01" value={form.salePrice} onChange={(e) => update('salePrice', e.target.value)} />
          {errors.salePrice && <div className="form-error">{errors.salePrice}</div>}
        </div>

        {marginPercent !== null && (
          <div className="form-live-margin">هامش الربح: {marginPercent.toFixed(1)}%</div>
        )}

        <div className="form-group">
          <label>الكمية الحالية</label>
          <input type="number" step="1" value={form.quantity} onChange={(e) => update('quantity', e.target.value)} />
          {errors.quantity && <div className="form-error">{errors.quantity}</div>}
        </div>

        <div className="form-group">
          <label>تاريخ آخر بيع (اختياري)</label>
          <input type="date" value={form.lastSaleDate} onChange={(e) => update('lastSaleDate', e.target.value)} />
        </div>

        <div className="form-group">
          <label>تاريخ انتهاء الصلاحية (اختياري)</label>
          <input type="date" value={form.expiryDate} onChange={(e) => update('expiryDate', e.target.value)} />
        </div>

        <div className="form-actions">
          <button type="button" className="btn-secondary" onClick={onClose}>إلغاء</button>
          <button type="submit" className="btn-primary" disabled={submitting}>
            {submitting ? 'جارِ الحفظ...' : 'حفظ الصنف'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
```

- [ ] **Step 2: Create `InventoryPage.tsx`**

```tsx
import { useEffect, useState } from 'react';
import { Sidebar } from '../components/Sidebar';
import { AddItemModal } from '../components/AddItemModal';
import { fetchItems, type ItemDto } from '../api/client';

const TABS: { key: 'ALL' | ItemDto['movementStatus']; label: string }[] = [
  { key: 'ALL', label: 'الكل' },
  { key: 'FAST', label: 'سريع الحركة' },
  { key: 'MEDIUM', label: 'متوسط الحركة' },
  { key: 'SLOW', label: 'بطيء الحركة' },
  { key: 'STAGNANT', label: 'راكد' },
];

const statusLabel: Record<string, string> = { FAST: 'سريع', MEDIUM: 'متوسط', SLOW: 'بطيء', STAGNANT: 'راكد' };

function isNearExpiry(expiryDate: string | null): boolean {
  if (!expiryDate) return false;
  const days = (new Date(expiryDate).getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24);
  return days >= 0 && days < 30;
}

export function InventoryPage() {
  const [items, setItems] = useState<ItemDto[] | null>(null);
  const [tab, setTab] = useState<'ALL' | ItemDto['movementStatus']>('ALL');
  const [showAddModal, setShowAddModal] = useState(false);

  useEffect(() => {
    fetchItems(1).then(setItems);
  }, []);

  const filtered = items?.filter((item) => tab === 'ALL' || item.movementStatus === tab) ?? [];

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-area">
        <div className="page-header">
          <div className="page-title">المخزون</div>
          <button className="btn-primary" onClick={() => setShowAddModal(true)}>+ إضافة صنف</button>
        </div>

        <div className="tabs">
          {TABS.map((t) => (
            <div
              key={t.key}
              className={`tab ${tab === t.key ? 'active' : ''}`}
              onClick={() => setTab(t.key)}
            >
              {t.label}
            </div>
          ))}
        </div>

        <div className="card">
          {items === null && <p style={{ color: 'var(--text-secondary)' }}>جاري تحميل الأصناف...</p>}
          {items !== null && filtered.length === 0 && (
            <p style={{ color: 'var(--text-secondary)' }}>لا توجد أصناف في هذا التصنيف.</p>
          )}
          {items !== null && filtered.length > 0 && (
            <table className="attention-table">
              <thead>
                <tr>
                  <th>الاسم</th>
                  <th>الفئة الفرعية</th>
                  <th>سعر التكلفة</th>
                  <th>سعر البيع</th>
                  <th>هامش الربح</th>
                  <th>الكمية</th>
                  <th>القيمة</th>
                  <th>تاريخ آخر بيع</th>
                  <th>تاريخ الانتهاء</th>
                  <th>الحالة</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((item) => (
                  <tr key={item.id} className={isNearExpiry(item.expiryDate) ? 'row-warning' : ''}>
                    <td>{item.name}</td>
                    <td>{item.subCategory ?? '-'}</td>
                    <td>{item.costPrice.toLocaleString('ar')}</td>
                    <td>{item.salePrice.toLocaleString('ar')}</td>
                    <td>{item.marginPercent.toFixed(1)}%</td>
                    <td>{item.quantity.toLocaleString('ar')}</td>
                    <td>{Math.round(item.inventoryValue).toLocaleString('ar')}</td>
                    <td>{item.lastSaleDate ?? '-'}</td>
                    <td>{item.expiryDate ?? '-'}</td>
                    <td><span className={`status-chip status-${item.movementStatus}`}>{statusLabel[item.movementStatus]}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </main>

      {showAddModal && (
        <AddItemModal
          onClose={() => setShowAddModal(false)}
          onCreated={(item) => setItems((prev) => (prev ? [...prev, item] : [item]))}
        />
      )}
    </div>
  );
}
```

Note: `Sidebar` is called here with no `active` prop — this only compiles once Task 7 changes `Sidebar`'s signature to derive its active item from the route instead of a prop. Do Task 7 in the same session before trying to build/verify this task in isolation, or temporarily pass `active="inventory"` if verifying this task standalone before Task 7 lands.

- [ ] **Step 3: Verify it compiles**

```bash
cd "C:/Users/m.domidi_freightos/Desktop/trust-platform/trust-platform/trust-frontend"
npx tsc -b --noEmit
```

Expected: no errors once Task 7 has also been applied (see note above). If verifying Task 5 before Task 7, expect a type error on the `<Sidebar />` call — that's fine, it resolves once Task 7 lands.

- [ ] **Step 4: Done**

---

### Task 6: `DailyEntryModal` and Dashboard topbar button

**Files:**
- Create: `trust-frontend/src/components/DailyEntryModal.tsx`
- Modify: `trust-frontend/src/pages/Dashboard.tsx`

**Interfaces:**
- Consumes: `submitDailyEntry`, `DailyEntryRequest` from `../api/client` (Task 2); `Modal` from `./Modal` (Task 3).
- Produces: `DailyEntryModal` component with props `{ onClose: () => void; onSubmitted: () => void }` — `onSubmitted` is called after a successful save so `Dashboard.tsx` can refetch.

- [ ] **Step 1: Create `DailyEntryModal.tsx`**

```tsx
import { useState, type FormEvent } from 'react';
import { Modal } from './Modal';
import { submitDailyEntry } from '../api/client';

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

interface FormState {
  entryDate: string;
  totalSales: string;
  totalCogs: string;
  totalProfit: string;
  profitTouched: boolean;
  availableLiquidity: string;
  receivables: string;
  payables: string;
}

const initialForm: FormState = {
  entryDate: today(),
  totalSales: '',
  totalCogs: '',
  totalProfit: '',
  profitTouched: false,
  availableLiquidity: '',
  receivables: '',
  payables: '',
};

export function DailyEntryModal({ onClose, onSubmitted }: { onClose: () => void; onSubmitted: () => void }) {
  const [form, setForm] = useState<FormState>(initialForm);
  const [errors, setErrors] = useState<Partial<Record<keyof FormState, string>>>({});
  const [apiError, setApiError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const sales = parseFloat(form.totalSales);
  const cogs = parseFloat(form.totalCogs);
  const computedProfit = !isNaN(sales) && !isNaN(cogs) ? sales - cogs : null;
  const effectiveProfit = form.profitTouched && form.totalProfit !== ''
    ? parseFloat(form.totalProfit)
    : computedProfit;
  const marginPercent = sales > 0 && effectiveProfit !== null && !isNaN(effectiveProfit)
    ? (effectiveProfit / sales) * 100
    : null;

  function update<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  function validate(): boolean {
    const next: Partial<Record<keyof FormState, string>> = {};
    if (!form.entryDate) next.entryDate = 'التاريخ مطلوب';
    if (!(parseFloat(form.totalSales) >= 0)) next.totalSales = 'إجمالي المبيعات مطلوب';
    if (!(parseFloat(form.totalCogs) >= 0)) next.totalCogs = 'تكلفة البضاعة المباعة مطلوبة';
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setApiError(null);
    if (!validate()) return;
    setSubmitting(true);
    try {
      await submitDailyEntry({
        branchId: 1,
        entryDate: form.entryDate,
        totalSales: parseFloat(form.totalSales),
        totalCogs: parseFloat(form.totalCogs),
        totalProfit: form.profitTouched && form.totalProfit !== '' ? parseFloat(form.totalProfit) : null,
        availableLiquidity: form.availableLiquidity === '' ? 0 : parseFloat(form.availableLiquidity),
        receivables: form.receivables === '' ? 0 : parseFloat(form.receivables),
        payables: form.payables === '' ? 0 : parseFloat(form.payables),
      });
      onSubmitted();
      onClose();
    } catch (err) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setApiError(message || 'تعذّر حفظ بيانات اليوم. حاول مرة أخرى.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title="إدخال بيانات اليوم" onClose={onClose}>
      <form onSubmit={handleSubmit}>
        {apiError && <div className="form-banner-error">{apiError}</div>}

        <div className="form-group">
          <label>التاريخ</label>
          <input type="date" value={form.entryDate} onChange={(e) => update('entryDate', e.target.value)} />
          {errors.entryDate && <div className="form-error">{errors.entryDate}</div>}
        </div>

        <div className="form-group">
          <label>إجمالي المبيعات</label>
          <input type="number" step="0.01" value={form.totalSales} onChange={(e) => update('totalSales', e.target.value)} />
          {errors.totalSales && <div className="form-error">{errors.totalSales}</div>}
        </div>

        <div className="form-group">
          <label>إجمالي تكلفة البضاعة المباعة (COGS)</label>
          <input type="number" step="0.01" value={form.totalCogs} onChange={(e) => update('totalCogs', e.target.value)} />
          {errors.totalCogs && <div className="form-error">{errors.totalCogs}</div>}
        </div>

        <div className="form-group">
          <label>إجمالي الربح (محسوب تلقائيًا، قابل للتعديل)</label>
          <input
            type="number"
            step="0.01"
            value={form.profitTouched ? form.totalProfit : (computedProfit !== null ? computedProfit.toFixed(2) : '')}
            onChange={(e) => { update('profitTouched', true); update('totalProfit', e.target.value); }}
          />
        </div>

        {marginPercent !== null && (
          <div className="form-live-margin">هامش الربح: {marginPercent.toFixed(1)}%</div>
        )}

        <div className="form-group">
          <label>السيولة المتاحة (اختياري)</label>
          <input type="number" step="0.01" value={form.availableLiquidity} onChange={(e) => update('availableLiquidity', e.target.value)} />
        </div>

        <div className="form-group">
          <label>الذمم المدينة (اختياري)</label>
          <input type="number" step="0.01" value={form.receivables} onChange={(e) => update('receivables', e.target.value)} />
        </div>

        <div className="form-group">
          <label>الالتزامات الحالة (اختياري)</label>
          <input type="number" step="0.01" value={form.payables} onChange={(e) => update('payables', e.target.value)} />
        </div>

        <div className="form-actions">
          <button type="button" className="btn-secondary" onClick={onClose}>إلغاء</button>
          <button type="submit" className="btn-primary" disabled={submitting}>
            {submitting ? 'جارِ الحفظ...' : 'حفظ'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
```

- [ ] **Step 2: Wire the trigger button and refetch into `Dashboard.tsx`**

Add imports near the top of `Dashboard.tsx`:

```tsx
import { useState } from 'react';
```
(already imported alongside `useEffect` — extend the existing import line `import { useEffect, useState } from 'react';` if not already combined.)

```tsx
import { DailyEntryModal } from '../components/DailyEntryModal';
```

Inside the `Dashboard` function component, add state:

```tsx
const [showEntryModal, setShowEntryModal] = useState(false);
```

Extract the dashboard fetch into a reusable function so it can be re-run after a successful submit. Replace:

```tsx
  useEffect(() => {
    fetchDashboard({ organizationId: 1 })
      .then(setData)
      .catch(() => {
        // في حال تعذّر الوصول للـ backend (مثلًا أثناء المعاينة بدون تشغيل السيرفر)
        // نعرض بيانات تجريبية بنفس الشكل حتى تبقى الواجهة قابلة للمعاينة الفورية.
        setData(mockDashboard);
        setUsingMock(true);
      });
  }, []);
```

with:

```tsx
  function loadDashboard() {
    fetchDashboard({ organizationId: 1 })
      .then((d) => { setData(d); setUsingMock(false); })
      .catch(() => {
        // في حال تعذّر الوصول للـ backend (مثلًا أثناء المعاينة بدون تشغيل السيرفر)
        // نعرض بيانات تجريبية بنفس الشكل حتى تبقى الواجهة قابلة للمعاينة الفورية.
        setData(mockDashboard);
        setUsingMock(true);
      });
  }

  useEffect(() => {
    loadDashboard();
  }, []);
```

Add the trigger button inside the `.topbar` `.user-chip` area — insert a new sibling button just before the closing `</div>` of the `.topbar` div (i.e. after the existing `.user-chip` div, still inside `.topbar`):

```tsx
          <button className="btn-primary" onClick={() => setShowEntryModal(true)}>+ إدخال بيانات اليوم</button>
```

Add the modal render at the end of the component, just before the final closing `</div>` of `.app-shell` (after `.footer-banner`'s closing tag, still inside `<main>`... place it as a sibling of `<main>` inside `.app-shell`, after `</main>`):

```tsx
      {showEntryModal && (
        <DailyEntryModal
          onClose={() => setShowEntryModal(false)}
          onSubmitted={loadDashboard}
        />
      )}
```

- [ ] **Step 3: Verify it compiles**

```bash
cd "C:/Users/m.domidi_freightos/Desktop/trust-platform/trust-platform/trust-frontend"
npx tsc -b --noEmit
```

Expected: no errors (again, modulo the `Sidebar` prop mismatch until Task 7 lands).

- [ ] **Step 4: Done**

---

### Task 7: Wire up `react-router-dom` and update `Sidebar`

**Files:**
- Modify: `trust-frontend/src/main.tsx`
- Modify: `trust-frontend/src/App.tsx`
- Modify: `trust-frontend/src/components/Sidebar.tsx`
- Modify: `trust-frontend/src/pages/Dashboard.tsx`

**Interfaces:**
- Produces: `Sidebar` component with **no required props** (was `{ active: string }`) — it derives the active key from `useLocation()` internally. This is a breaking change to `Sidebar`'s public signature; Tasks 5 and 6's `<Sidebar />` calls (no props) are what this enables.

- [ ] **Step 1: Wrap the app in `BrowserRouter`**

Replace the full contents of `trust-frontend/src/main.tsx`:

```tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </React.StrictMode>
);
```

- [ ] **Step 2: Add routes to `App.tsx`**

Replace the full contents of `trust-frontend/src/App.tsx`:

```tsx
import { Routes, Route } from 'react-router-dom';
import { Dashboard } from './pages/Dashboard';
import { InventoryPage } from './pages/InventoryPage';
import './styles/theme.css';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Dashboard />} />
      <Route path="/inventory" element={<InventoryPage />} />
    </Routes>
  );
}
```

- [ ] **Step 3: Update `Sidebar.tsx` to derive active state from the route**

Replace the full contents of `trust-frontend/src/components/Sidebar.tsx`:

```tsx
import { Link, useLocation } from 'react-router-dom';

interface NavItem {
  key: string;
  label: string;
  icon: string;
  badge?: number;
  path?: string;
}

const items: NavItem[] = [
  { key: 'home', label: 'الرئيسية', icon: '🏠', path: '/' },
  { key: 'sales', label: 'المبيعات', icon: '📈' },
  { key: 'inventory', label: 'المخزون', icon: '📦', path: '/inventory' },
  { key: 'purchases', label: 'المشتريات', icon: '🛒' },
  { key: 'profitability', label: 'الربحية', icon: '💰' },
  { key: 'liquidity', label: 'السيولة', icon: '💵' },
  { key: 'pricing', label: 'التسعير', icon: '🏷️' },
  { key: 'reports', label: 'التقارير', icon: '📄' },
  { key: 'notifications', label: 'التنبيهات', icon: '🔔', badge: 12 },
  { key: 'suppliers', label: 'الموردون', icon: '🚚' },
  { key: 'settings', label: 'الإعدادات', icon: '⚙️' },
];

export function Sidebar() {
  const location = useLocation();

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <div className="logo">T</div>
        <div className="titles">
          <div className="name">TRUST</div>
          <div className="subtitle">المدير التجاري الذكي</div>
        </div>
      </div>
      {items.map((item) => {
        const isActive = item.path === location.pathname;
        const content = (
          <>
            <span>{item.icon}</span>
            <span>{item.label}</span>
            {item.badge && <span className="badge">{item.badge}</span>}
          </>
        );
        return item.path ? (
          <Link key={item.key} to={item.path} className={`nav-item ${isActive ? 'active' : ''}`}>
            {content}
          </Link>
        ) : (
          <div key={item.key} className="nav-item">
            {content}
          </div>
        );
      })}
    </aside>
  );
}
```

Note: `.nav-item` CSS in `theme.css` currently applies to `div`s; since `<Link>` renders an `<a>` tag, add this to `theme.css` (append) so anchor-based nav items don't get default link styling:

```css
a.nav-item {
  text-decoration: none;
}
```

- [ ] **Step 4: Update `Dashboard.tsx`'s `<Sidebar>` call**

In `trust-frontend/src/pages/Dashboard.tsx`, find both occurrences of `<Sidebar active="home" />` (one in the loading-state branch, one in the main render) and replace each with `<Sidebar />`.

- [ ] **Step 5: Verify it compiles**

```bash
cd "C:/Users/m.domidi_freightos/Desktop/trust-platform/trust-platform/trust-frontend"
npx tsc -b --noEmit
```

Expected: no errors.

- [ ] **Step 6: Done**

---

### Task 8: End-to-end manual verification

**Files:** none (verification only).

- [ ] **Step 1: Confirm dev servers are running**

Backend on `http://localhost:8080` (restarted in Task 1 with the DailyEntryController fix), frontend on `http://localhost:5173`.

- [ ] **Step 2: Verify routing**

Open `http://localhost:5173/` in a browser. Click "المخزون" in the sidebar — URL should change to `/inventory` without a full page reload, and "المخزون" should be visually highlighted as active. Click "الرئيسية" — should navigate back to `/` and highlight it instead.

- [ ] **Step 3: Verify Inventory page**

On `/inventory`: confirm the 6 seeded items appear under "الكل". Click through each movement-status tab and confirm the table filters correctly (cross-check counts against `curl -s "http://localhost:8080/api/items?branchId=1" | grep -o '"movementStatus":"[A-Z]*"' | sort | uniq -c`). Click "+ إضافة صنف", fill in a new item (e.g. name "صنف تجريبي", cost 5, sale 10, quantity 20), confirm the live margin preview shows 50.0%, submit, confirm the modal closes and the new item appears in the table under the "سريع الحركة" tab (default `movementStatus` is `FAST` per the `Item` entity).

- [ ] **Step 4: Verify Daily Entry modal**

On `/`, click "+ إدخال بيانات اليوم". Enter sales 2000, COGS 1200, confirm computed profit shows 800 and margin preview shows 40.0%. Submit. Confirm the modal closes and the dashboard's metric cards refresh (compare "المبيعات اليوم" before/after — note this only visibly changes if `entryDate` equals the date the dashboard's "today" calculation uses; if it doesn't visibly change, confirm via `curl -s "http://localhost:8080/api/entries/daily" ...` is not available (no GET endpoint) — instead confirm indirectly by resubmitting the same date with different numbers and checking the dashboard's `salesToday` value changes accordingly after refetch).

- [ ] **Step 5: Verify Health gauge**

Confirm the "صحة الأعمال" card on `/` renders a circular ring (not plain text) with the score centered inside and the status label below.

- [ ] **Step 6: Done** (no git repo — nothing to commit)
