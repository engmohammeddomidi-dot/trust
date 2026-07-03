import { useEffect, useState } from 'react';
import { Sidebar } from '../components/Sidebar';
import { AddItemModal } from '../components/AddItemModal';
import { ImportItemsCsvModal } from '../components/ImportItemsCsvModal';
import { fetchItems, fetchSuppliers, linkItemSupplier, type ItemDto, type SupplierDto } from '../api/client';
import { requireBranchId, requireOrganizationId } from '../auth/session';

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
  const [suppliers, setSuppliers] = useState<SupplierDto[] | null>(null);
  const [tab, setTab] = useState<'ALL' | ItemDto['movementStatus']>('ALL');
  const [showAddModal, setShowAddModal] = useState(false);
  const [showImportModal, setShowImportModal] = useState(false);
  const [savingSupplierFor, setSavingSupplierFor] = useState<number | null>(null);

  function load() {
    fetchItems(requireBranchId()).then(setItems);
    fetchSuppliers(requireOrganizationId()).then(setSuppliers);
  }

  useEffect(() => {
    load();
  }, []);

  async function handleSupplierChange(item: ItemDto, supplierId: string) {
    if (!supplierId) return;
    setSavingSupplierFor(item.id);
    try {
      const updated = await linkItemSupplier(item.id, Number(supplierId), item.safetyStockDays);
      setItems((prev) => prev?.map((i) => (i.id === updated.id ? updated : i)) ?? null);
    } finally {
      setSavingSupplierFor(null);
    }
  }

  async function handleSafetyStockBlur(item: ItemDto, value: string) {
    const days = Number(value);
    if (!Number.isFinite(days) || days < 0 || days === item.safetyStockDays || !item.supplierId) return;
    setSavingSupplierFor(item.id);
    try {
      const updated = await linkItemSupplier(item.id, item.supplierId, days);
      setItems((prev) => prev?.map((i) => (i.id === updated.id ? updated : i)) ?? null);
    } finally {
      setSavingSupplierFor(null);
    }
  }

  const filtered = items?.filter((item) => tab === 'ALL' || item.movementStatus === tab) ?? [];

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-area">
        <div className="page-header">
          <div className="page-title">المخزون</div>
          <div style={{ display: 'flex', gap: 10 }}>
            <button className="btn-secondary" onClick={() => setShowImportModal(true)}>⬆ استيراد CSV</button>
            <button className="btn-primary" onClick={() => setShowAddModal(true)}>+ إضافة صنف</button>
          </div>
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
                  <th>المورد المفضّل</th>
                  <th>مخزون الأمان (يوم)</th>
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
                    <td>
                      <select
                        value={item.supplierId ?? ''}
                        disabled={savingSupplierFor === item.id || suppliers === null}
                        onChange={(e) => handleSupplierChange(item, e.target.value)}
                        style={{
                          background: 'var(--bg-panel-alt)', border: '1px solid var(--border-subtle)',
                          borderRadius: 'var(--radius-md)', padding: '4px 8px', color: 'var(--text-primary)', fontSize: 12,
                        }}
                      >
                        <option value="">— بدون مورد —</option>
                        {suppliers?.map((s) => (
                          <option key={s.id} value={s.id}>{s.name}</option>
                        ))}
                      </select>
                    </td>
                    <td>
                      <input
                        type="number"
                        min={0}
                        defaultValue={item.safetyStockDays}
                        disabled={!item.supplierId || savingSupplierFor === item.id}
                        onBlur={(e) => handleSafetyStockBlur(item, e.target.value)}
                        style={{
                          width: 60, background: 'var(--bg-panel-alt)', border: '1px solid var(--border-subtle)',
                          borderRadius: 'var(--radius-md)', padding: '4px 8px', color: 'var(--text-primary)', fontSize: 12,
                        }}
                      />
                    </td>
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
      {showImportModal && (
        <ImportItemsCsvModal
          onClose={() => setShowImportModal(false)}
          onImported={load}
        />
      )}
    </div>
  );
}
