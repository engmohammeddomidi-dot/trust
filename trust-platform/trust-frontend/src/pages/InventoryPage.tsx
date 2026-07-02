import { useEffect, useState } from 'react';
import { Sidebar } from '../components/Sidebar';
import { AddItemModal } from '../components/AddItemModal';
import { fetchItems, type ItemDto } from '../api/client';
import { requireBranchId } from '../auth/session';

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
    fetchItems(requireBranchId()).then(setItems);
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
