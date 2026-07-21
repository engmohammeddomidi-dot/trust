import { useEffect, useState } from 'react';
import { AdminSidebar } from '../../components/AdminSidebar';
import { KpiCard } from '../../components/KpiCard';
import { SalesChart } from '../../components/SalesChart';
import { HealthGauge } from '../../components/HealthGauge';
import { fetchAdminOverview, type AdminOverviewDto } from '../../api/client';

function turnoverLabel(rate: number): string {
  if (rate >= 85) return 'ممتاز';
  if (rate >= 70) return 'جيد جدًا';
  if (rate >= 50) return 'جيد';
  if (rate >= 30) return 'مقبول';
  return 'ضعيف';
}

const categoryLabel: Record<string, string> = {
  SUPERMARKET: 'سوبرماركت',
  PHARMACY: 'صيدلية',
  RESTAURANT: 'مطعم',
  RETAIL_CLOTHING: 'تجارة ملابس',
  GENERAL_TRADE: 'تجارة عامة',
  COMPANY_OTHER: 'أخرى',
};

function scoreColor(score: number): string {
  if (score >= 61) return 'var(--accent-green)';
  if (score >= 41) return 'var(--accent-amber)';
  return 'var(--accent-red)';
}

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

            <div className="grid-row grid-2" style={{ marginTop: 14, marginBottom: 14 }}>
              <div className="card">
                <div className="card-title">المخاطر والفرص المفتوحة على المنصة</div>
                <div className="grid-row grid-2" style={{ marginBottom: 0 }}>
                  <div style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--accent-red)' }}>{overview.riskOpportunity.openRisksCount}</div>
                    <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>مخاطر مفتوحة</div>
                    <div style={{ fontSize: 13, color: 'var(--accent-red)', marginTop: 4 }}>
                      {Math.round(overview.riskOpportunity.openRisksValue).toLocaleString('ar')} شيكل
                    </div>
                  </div>
                  <div style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--accent-green)' }}>{overview.riskOpportunity.openOpportunitiesCount}</div>
                    <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>فرص مفتوحة</div>
                    <div style={{ fontSize: 13, color: 'var(--accent-green)', marginTop: 4 }}>
                      {Math.round(overview.riskOpportunity.openOpportunitiesValue).toLocaleString('ar')} شيكل
                    </div>
                  </div>
                </div>
              </div>
              <div className="card">
                <div className="card-title">توزيع صحة الأعمال بين المؤسسات</div>
                <div className="grid-row grid-3" style={{ marginBottom: 0 }}>
                  <div style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--accent-green)' }}>{overview.healthDistribution.good}</div>
                    <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>جيدة (61+)</div>
                  </div>
                  <div style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--accent-amber)' }}>{overview.healthDistribution.medium}</div>
                    <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>متوسطة (41-60)</div>
                  </div>
                  <div style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--accent-red)' }}>{overview.healthDistribution.poor}</div>
                    <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>ضعيفة (تحت 41)</div>
                  </div>
                </div>
              </div>
            </div>

            <div className="card">
              <div className="card-title">الأعلى أداءً بين المؤسسات</div>
              {overview.leaderboard.length === 0 ? (
                <p style={{ color: 'var(--text-secondary)', fontSize: 12 }}>لا توجد بيانات كافية بعد</p>
              ) : (
                <table className="attention-table">
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>المؤسسة</th>
                      <th>التصنيف</th>
                      <th>صحة الأعمال</th>
                    </tr>
                  </thead>
                  <tbody>
                    {overview.leaderboard.map((org, i) => (
                      <tr key={org.id}>
                        <td>{i + 1}</td>
                        <td>{org.name}</td>
                        <td>{categoryLabel[org.category] ?? org.category}</td>
                        <td style={{ color: scoreColor(org.avgHealthScore), fontWeight: 700 }}>{org.avgHealthScore}/100</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>

            <div className="grid-row grid-2" style={{ marginTop: 14 }}>
              <div className="card">
                <div className="card-title">مؤشر الأداء والأثر الفعلي على المنصة</div>
                {overview.performanceImpactSummary.performanceScore !== null ? (
                  <HealthGauge
                    score={overview.performanceImpactSummary.performanceScore}
                    label={turnoverLabel(overview.performanceImpactSummary.performanceScore)}
                  />
                ) : (
                  <div style={{ textAlign: 'center', padding: '10px 0' }}>
                    <div style={{ fontSize: 22, fontWeight: 700, color: 'var(--text-secondary)' }}>—</div>
                    <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginTop: 6 }}>لا توجد طلبيات مستلمة بعد لحساب المؤشر</p>
                  </div>
                )}
              </div>
              <div className="card">
                <div className="card-title">أعلى التوصيات قيمةً على المنصة</div>
                {overview.topRecommendations.length === 0 ? (
                  <p style={{ color: 'var(--text-secondary)', fontSize: 12 }}>لا توجد توصيات مفتوحة حاليًا</p>
                ) : (
                  overview.topRecommendations.map((rec) => (
                    <div className="recommendation-row" key={rec.id}>
                      <span className={`priority-tag ${rec.category === 'RISK' ? 'priority-HIGH' : 'priority-LOW'}`}>
                        {rec.category === 'RISK' ? 'خطر' : 'فرصة'}
                      </span>
                      <span className="rec-title">{rec.organizationName} — {rec.itemName}</span>
                      <span className="rec-value">{Math.round(rec.financialImpact).toLocaleString('ar')} شيكل</span>
                    </div>
                  ))
                )}
              </div>
            </div>
          </>
        )}
      </main>
    </div>
  );
}
