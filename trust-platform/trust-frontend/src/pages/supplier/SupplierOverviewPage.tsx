import { useEffect, useState } from 'react';
import { SupplierSidebar } from '../../components/SupplierSidebar';
import { KpiCard } from '../../components/KpiCard';
import { fetchSupplierOverview, type SupplierPortalOverviewDto } from '../../api/client';

const statusLabel: Record<string, string> = {
  SENT: 'بانتظار التوريد',
  RECEIVED: 'تم الاستلام',
};
const statusClass: Record<string, string> = {
  SENT: 'status-MEDIUM',
  RECEIVED: 'status-FAST',
};

export function SupplierOverviewPage() {
  const [overview, setOverview] = useState<SupplierPortalOverviewDto | null>(null);

  useEffect(() => {
    fetchSupplierOverview().then(setOverview);
  }, []);

  return (
    <div className="app-shell">
      <SupplierSidebar />
      <main className="main-area">
        <div className="page-header">
          <div className="page-title">نظرة عامة</div>
        </div>

        {overview === null ? (
          <p style={{ color: 'var(--text-secondary)' }}>جاري التحميل...</p>
        ) : overview.organizationsServedCount === 0 ? (
          <div className="card">
            <p style={{ color: 'var(--text-secondary)' }}>
              لا يوجد بعد أي مؤسسة تربط حساب المورد هذا بسجل مورّد لديها. تواصل مع المؤسسات التي تتعامل
              معها لإضافة بريدك الإلكتروني ({overview.supplierName}) في ملف المورد الخاص بك عندهم.
            </p>
          </div>
        ) : (
          <>
            <div className="grid-row grid-4" style={{ marginBottom: 14 }}>
              <KpiCard icon="🏢" iconBg="var(--accent-blue)" label="المؤسسات المتعامل معها"
                value={String(overview.organizationsServedCount)} caption="عبر المنصة" />
              <KpiCard icon="📦" iconBg="var(--accent-amber)" label="طلبات بانتظار التوريد"
                value={String(overview.openOrdersCount)} unit="طلب"
                caption={`${Math.round(overview.openOrdersValue).toLocaleString('ar')} شيكل`} captionColor="var(--accent-amber)" />
              <KpiCard icon="✅" iconBg="var(--accent-green)" label="طلبات مستلمة"
                value={String(overview.receivedOrdersCount)} unit="طلب"
                caption={`${Math.round(overview.totalReceivedValue).toLocaleString('ar')} شيكل`} captionColor="var(--accent-green)" />
              <KpiCard icon="⭐" iconBg="var(--accent-purple)" label="متوسط التقييم"
                value={overview.avgRating !== null ? String(overview.avgRating) : '—'} unit={overview.avgRating !== null ? '/100' : undefined}
                caption="حسب تقييم كل مؤسسة" />
            </div>

            <div className="card">
              <div className="card-title">أحدث الطلبات</div>
              {overview.recentOrders.length === 0 ? (
                <p style={{ color: 'var(--text-secondary)', fontSize: 12 }}>لا توجد طلبات مسجّلة بعد</p>
              ) : (
                <table className="attention-table">
                  <thead>
                    <tr>
                      <th>المؤسسة</th>
                      <th>الفرع</th>
                      <th>الصنف</th>
                      <th>الكمية</th>
                      <th>سعر الوحدة</th>
                      <th>الإجمالي</th>
                      <th>الحالة</th>
                      <th>تاريخ الطلب</th>
                      <th>التسليم المتوقع / تاريخ الاستلام</th>
                    </tr>
                  </thead>
                  <tbody>
                    {overview.recentOrders.map((o) => (
                      <tr key={o.id}>
                        <td>{o.organizationName}</td>
                        <td>{o.branchName}</td>
                        <td>{o.itemName ?? '-'}</td>
                        <td>{o.quantity.toLocaleString('ar')}</td>
                        <td>{o.costPrice.toLocaleString('ar')} شيكل</td>
                        <td>{Math.round(o.quantity * o.costPrice).toLocaleString('ar')} شيكل</td>
                        <td><span className={`status-chip ${statusClass[o.status]}`}>{statusLabel[o.status]}</span></td>
                        <td>{o.purchaseDate}</td>
                        <td>{o.status === 'RECEIVED' ? (o.receivedDate ?? '-') : o.expectedDeliveryDate}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </>
        )}
      </main>
    </div>
  );
}
