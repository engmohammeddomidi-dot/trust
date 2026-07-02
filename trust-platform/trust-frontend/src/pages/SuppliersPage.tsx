import { useEffect, useState } from 'react';
import { Sidebar } from '../components/Sidebar';
import { fetchMyGroupOrderParticipation, fetchPurchases, type GroupOrderParticipationDto, type PurchaseDto } from '../api/client';
import { requireBranchId } from '../auth/session';

interface SupplierSummary {
  name: string;
  purchaseCount: number;
  totalSpend: number;
  items: Set<string>;
  lastPurchaseDate: string;
}

function summarize(purchases: PurchaseDto[]): SupplierSummary[] {
  const map = new Map<string, SupplierSummary>();
  for (const p of purchases) {
    const existing = map.get(p.supplierName);
    if (existing) {
      existing.purchaseCount += 1;
      existing.totalSpend += p.totalCost;
      if (p.itemName) existing.items.add(p.itemName);
      if (p.purchaseDate > existing.lastPurchaseDate) existing.lastPurchaseDate = p.purchaseDate;
    } else {
      map.set(p.supplierName, {
        name: p.supplierName,
        purchaseCount: 1,
        totalSpend: p.totalCost,
        items: new Set(p.itemName ? [p.itemName] : []),
        lastPurchaseDate: p.purchaseDate,
      });
    }
  }
  return [...map.values()].sort((a, b) => b.totalSpend - a.totalSpend);
}

const statusLabel: Record<string, string> = {
  COLLECTING: 'تجميع',
  NEGOTIATED: 'تم التفاوض',
  DISTRIBUTED: 'تم التوزيع',
  CANCELLED: 'ملغى',
};
const statusClass: Record<string, string> = {
  COLLECTING: 'status-SLOW',
  NEGOTIATED: 'status-MEDIUM',
  DISTRIBUTED: 'status-FAST',
  CANCELLED: 'status-STAGNANT',
};

export function SuppliersPage() {
  const [purchases, setPurchases] = useState<PurchaseDto[] | null>(null);
  const [participations, setParticipations] = useState<GroupOrderParticipationDto[] | null>(null);

  useEffect(() => {
    fetchPurchases(requireBranchId()).then(setPurchases);
    fetchMyGroupOrderParticipation().then(setParticipations);
  }, []);

  const suppliers = purchases ? summarize(purchases) : [];

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-area">
        <div className="page-header">
          <div className="page-title">الموردون</div>
        </div>

        <div className="card" style={{ marginBottom: 14 }}>
          <div className="card-title">الموردون المسجّلون (مبنيّ على سجل المشتريات)</div>
          {purchases === null && <p style={{ color: 'var(--text-secondary)' }}>جاري التحميل...</p>}
          {purchases !== null && suppliers.length === 0 && (
            <p style={{ color: 'var(--text-secondary)' }}>
              لا يوجد موردون بعد — يُضاف المورد تلقائيًا عند تسجيل أول عملية شراء من صفحة المشتريات.
            </p>
          )}
          {suppliers.length > 0 && (
            <table className="attention-table">
              <thead>
                <tr>
                  <th>المورد</th>
                  <th>عدد الطلبات</th>
                  <th>إجمالي المشتريات</th>
                  <th>الأصناف الموردة</th>
                  <th>آخر عملية شراء</th>
                </tr>
              </thead>
              <tbody>
                {suppliers.map((s) => (
                  <tr key={s.name}>
                    <td>{s.name}</td>
                    <td>{s.purchaseCount}</td>
                    <td>{Math.round(s.totalSpend).toLocaleString('ar')} شيكل</td>
                    <td>{s.items.size > 0 ? [...s.items].join('، ') : '-'}</td>
                    <td>{s.lastPurchaseDate}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="card">
          <div className="card-title">مشاركاتي في الطلبات الجماعية 🤝</div>
          {participations === null && <p style={{ color: 'var(--text-secondary)' }}>جاري التحميل...</p>}
          {participations !== null && participations.length === 0 && (
            <p style={{ color: 'var(--text-secondary)' }}>
              لا توجد مشاركات بعد — يمكنك الانضمام لطلب شراء جماعي من صفحة المشتريات.
            </p>
          )}
          {participations !== null && participations.length > 0 && (
            <table className="attention-table">
              <thead>
                <tr>
                  <th>الصنف</th>
                  <th>الكمية</th>
                  <th>السعر الفردي</th>
                  <th>السعر بالجملة</th>
                  <th>التوفير المحقق</th>
                  <th>الحالة</th>
                </tr>
              </thead>
              <tbody>
                {participations.map((p, idx) => (
                  <tr key={idx}>
                    <td>{p.itemName}</td>
                    <td>{p.quantity.toLocaleString('ar')}</td>
                    <td>{p.estimatedMarketPrice.toLocaleString('ar')}</td>
                    <td>{p.negotiatedPrice ? p.negotiatedPrice.toLocaleString('ar') : '-'}</td>
                    <td style={{ color: 'var(--accent-green)' }}>
                      {p.savings != null ? `${Math.round(p.savings).toLocaleString('ar')} شيكل` : '-'}
                    </td>
                    <td><span className={`status-chip ${statusClass[p.status]}`}>{statusLabel[p.status]}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </main>
    </div>
  );
}
