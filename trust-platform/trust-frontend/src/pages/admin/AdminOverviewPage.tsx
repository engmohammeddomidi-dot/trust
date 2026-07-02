import { useEffect, useState } from 'react';
import { AdminSidebar } from '../../components/AdminSidebar';
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
            <div className="grid-row grid-3" style={{ marginBottom: 14 }}>
              <div className="card">
                <div className="label">عدد المؤسسات النشطة</div>
                <div className="value" style={{ fontSize: 26, fontWeight: 700 }}>{overview.totalOrganizations}</div>
              </div>
              <div className="card">
                <div className="label">إجمالي الفروع</div>
                <div className="value" style={{ fontSize: 26, fontWeight: 700 }}>{overview.totalBranches}</div>
              </div>
              <div className="card">
                <div className="label">متوسط صحة الأعمال عبر المنصة</div>
                <div className="value" style={{ fontSize: 26, fontWeight: 700 }}>{overview.avgHealthScore}<span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>/100</span></div>
              </div>
            </div>

            <div className="grid-row grid-2">
              <div className="card">
                <div className="card-title">المؤسسات حسب التصنيف</div>
                {Object.entries(overview.organizationsByCategory).map(([cat, count]) => (
                  <div key={cat} className="recommendation-row">
                    <span className="rec-title">{categoryLabel[cat] ?? cat}</span>
                    <span className="rec-value">{count}</span>
                  </div>
                ))}
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
