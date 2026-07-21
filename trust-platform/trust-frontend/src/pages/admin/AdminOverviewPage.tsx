import { useEffect, useState } from 'react';
import { AdminSidebar } from '../../components/AdminSidebar';
import { KpiCard } from '../../components/KpiCard';
import { SalesChart } from '../../components/SalesChart';
import { fetchAdminOverview, type AdminOverviewDto } from '../../api/client';

const categoryLabel: Record<string, string> = {
  SUPERMARKET: 'سوبرماركت',
  PHARMACY: 'صيدلية',
  RESTAURANT: 'مطعم',
  RETAIL_CLOTHING: 'تجارة ملابس',
  GENERAL_TRADE: 'تجارة عامة',
  COMPANY_OTHER: 'أخرى',
};

export function AdminOverviewPage() {
  const [overview, setOverview] = useState<AdminOverviewDto | null>(null);

  useEffect(() => {
    fetchAdminOverview().then(setOverview);
  }, []);

  return (
    <div className="app-shell">
      <AdminSidebar />
      <main className="main-area">
        <div className="page-header">
          <div className="page-title">نظرة عامة على المنصة</div>
        </div>

        {overview === null ? (
          <p style={{ color: 'var(--text-secondary)' }}>جاري التحميل...</p>
        ) : (
          <>
            <div className="grid-row grid-4" style={{ marginBottom: 14 }}>
              <KpiCard icon="🏢" iconBg="var(--accent-blue)" label="عدد المؤسسات النشطة"
                value={String(overview.totalOrganizations)} caption="على مستوى المنصة" />
              <KpiCard icon="🏬" iconBg="var(--accent-blue)" label="إجمالي الفروع"
                value={String(overview.totalBranches)} caption="على مستوى المنصة" />
              <KpiCard icon="💚" iconBg="var(--accent-green)" label="متوسط صحة الأعمال"
                value={String(overview.avgHealthScore)} unit="/100" caption="آخر 30 يوم" />
              <KpiCard icon="📈" iconBg="var(--accent-purple)" label="مبيعات المنصة اليوم"
                value={Math.round(overview.totalSalesToday).toLocaleString('ar')} unit="شيكل" caption="جميع المؤسسات" />
            </div>

            <div className="grid-row grid-2" style={{ marginBottom: 14 }}>
              <SalesChart data={overview.salesTrend.map((p) => ({ date: p.date, sales: p.totalSales }))} />
              <div className="card">
                <div className="card-title">المؤسسات حسب التصنيف</div>
                {Object.entries(overview.organizationsByCategory).map(([cat, count]) => (
                  <div key={cat} className="recommendation-row">
                    <span className="rec-title">{categoryLabel[cat] ?? cat}</span>
                    <span className="rec-value">{count}</span>
                  </div>
                ))}
              </div>
            </div>

            <div className="grid-row grid-2">
              <div className="card">
                <div className="card-title">التوزيع الجغرافي حسب المدينة</div>
                {overview.cityBreakdown.length === 0 ? (
                  <p style={{ color: 'var(--text-secondary)', fontSize: 12 }}>لا توجد بيانات كافية بعد</p>
                ) : (
                  <table className="attention-table">
                    <thead>
                      <tr>
                        <th>المدينة</th>
                        <th>المؤسسات</th>
                        <th>الفروع</th>
                        <th>متوسط الصحة</th>
                      </tr>
                    </thead>
                    <tbody>
                      {overview.cityBreakdown.map((c) => (
                        <tr key={c.city}>
                          <td>{c.city}</td>
                          <td>{c.organizationCount}</td>
                          <td>{c.branchCount}</td>
                          <td>{c.avgHealthScore}/100</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
              <div className="card">
                <div className="label">إجمالي قيمة المخزون الراكد على المنصة</div>
                <div className="value" style={{ fontSize: 26, fontWeight: 700, color: 'var(--accent-red)' }}>
                  {Math.round(overview.totalStagnantValue).toLocaleString('ar')} <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>شيكل</span>
                </div>
                <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginTop: 10 }}>
                  فرصة محتملة لتصريف/شراء جماعي بين المؤسسات — راجع صفحة "الأصناف الراكدة"
                </p>
              </div>
            </div>
          </>
        )}
      </main>
    </div>
  );
}
