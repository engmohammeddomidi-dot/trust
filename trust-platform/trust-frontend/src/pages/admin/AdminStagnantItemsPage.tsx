import { useEffect, useState } from 'react';
import { AdminSidebar } from '../../components/AdminSidebar';
import { fetchAdminStagnantItems, type AdminStagnantItemDto } from '../../api/client';

interface ClearanceGroup {
  itemName: string;
  organizations: Set<string>;
  totalQuantity: number;
  totalValue: number;
}

function findClearanceOpportunities(items: AdminStagnantItemDto[]): ClearanceGroup[] {
  const map = new Map<string, ClearanceGroup>();
  for (const item of items) {
    const existing = map.get(item.itemName);
    if (existing) {
      existing.organizations.add(item.organizationName);
      existing.totalQuantity += item.quantity;
      existing.totalValue += item.inventoryValue;
    } else {
      map.set(item.itemName, {
        itemName: item.itemName,
        organizations: new Set([item.organizationName]),
        totalQuantity: item.quantity,
        totalValue: item.inventoryValue,
      });
    }
  }
  return [...map.values()]
    .filter((g) => g.organizations.size >= 2)
    .sort((a, b) => b.totalValue - a.totalValue);
}

export function AdminStagnantItemsPage() {
  const [items, setItems] = useState<AdminStagnantItemDto[] | null>(null);

  useEffect(() => {
    fetchAdminStagnantItems().then(setItems);
  }, []);

  const totalValue = items?.reduce((sum, i) => sum + i.inventoryValue, 0) ?? 0;
  const clearanceGroups = items ? findClearanceOpportunities(items) : [];
  const matchedItemNames = new Set(clearanceGroups.map((g) => g.itemName));

  return (
    <div className="app-shell">
      <AdminSidebar />
      <main className="main-area">
        <div className="page-header">
          <div className="page-title">الأصناف الراكدة عبر المنصة</div>
        </div>

        <div className="grid-row grid-2" style={{ marginBottom: 14 }}>
          <div className="card">
            <div className="label">إجمالي القيمة الراكدة</div>
            <div className="value" style={{ fontSize: 22, fontWeight: 700, color: 'var(--accent-red)' }}>
              {Math.round(totalValue).toLocaleString('ar')} <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>شيكل</span>
            </div>
          </div>
          <div className="card">
            <div className="label">عدد الأصناف الراكدة</div>
            <div className="value" style={{ fontSize: 22, fontWeight: 700 }}>{items?.length ?? 0}</div>
          </div>
        </div>

        {clearanceGroups.length > 0 && (
          <div className="card" style={{ marginBottom: 14 }}>
            <div className="card-title">فرص تصريف مشترك 💡</div>
            <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 14 }}>
              نفس الصنف راكد لدى أكثر من مؤسسة — فرصة لتجميع الكمية في صفقة تصريف أو شراء جماعي واحدة بدل التعامل مع كل مؤسسة منفردة.
            </p>
            {clearanceGroups.map((g) => (
              <div className="recommendation-row" key={g.itemName}>
                <span className="priority-tag priority-MEDIUM">{g.organizations.size} مؤسسات</span>
                <span className="rec-title">
                  {g.itemName} — كمية إجمالية {g.totalQuantity.toLocaleString('ar')}
                </span>
                <span className="rec-value">{Math.round(g.totalValue).toLocaleString('ar')} شيكل</span>
              </div>
            ))}
          </div>
        )}

        <div className="card">
          <div className="card-title">تجميع كل الأصناف الراكدة (مرتّبة حسب القيمة)</div>
          <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 14 }}>
            الأصناف المميّزة 💡 لديها فرصة تصريف مشترك أعلاه. أتمتة كاملة للمطابقة والتفاوض جزء من مرحلة لاحقة.
          </p>
          {items === null && <p style={{ color: 'var(--text-secondary)' }}>جاري التحميل...</p>}
          {items !== null && items.length === 0 && (
            <p style={{ color: 'var(--text-secondary)' }}>لا يوجد مخزون راكد على المنصة حاليًا.</p>
          )}
          {items !== null && items.length > 0 && (
            <table className="attention-table">
              <thead>
                <tr>
                  <th>الصنف</th>
                  <th>المؤسسة</th>
                  <th>الفرع</th>
                  <th>الكمية</th>
                  <th>القيمة</th>
                  <th>آخر بيع</th>
                </tr>
              </thead>
              <tbody>
                {items.map((i, idx) => (
                  <tr key={idx}>
                    <td>{matchedItemNames.has(i.itemName) ? `💡 ${i.itemName}` : i.itemName}</td>
                    <td>{i.organizationName}</td>
                    <td>{i.branchName}</td>
                    <td>{i.quantity.toLocaleString('ar')}</td>
                    <td>{Math.round(i.inventoryValue).toLocaleString('ar')}</td>
                    <td>{i.lastSaleDate ?? '-'}</td>
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
