import { useEffect, useState } from 'react';
import { Sidebar } from '../components/Sidebar';
import { MetricCard } from '../components/MetricCard';
import { KpiCard } from '../components/KpiCard';
import { HealthGauge } from '../components/HealthGauge';
import { HealthRadar } from '../components/HealthRadar';
import { SalesChart } from '../components/SalesChart';
import { RecommendationsList } from '../components/RecommendationsList';
import { DonutBreakdown } from '../components/DonutBreakdown';
import { AttentionTable } from '../components/AttentionTable';
import { DailyEntryModal } from '../components/DailyEntryModal';
import { NotificationBell } from '../components/NotificationBell';
import { applyRecommendation, fetchDashboard, fetchRecommendations, type DashboardResponse } from '../api/client';
import { mockDashboard } from '../api/mock';
import { getSession, requireBranchId, requireOrganizationId } from '../auth/session';

const arabicMonths = ['يناير', 'فبراير', 'مارس', 'أبريل', 'مايو', 'يونيو', 'يوليو', 'أغسطس', 'سبتمبر', 'أكتوبر', 'نوفمبر', 'ديسمبر'];

function todayLabel(): string {
  const d = new Date();
  return `${d.getDate()} ${arabicMonths[d.getMonth()]} ${d.getFullYear()}`;
}

function turnoverLabel(rate: number): string {
  if (rate >= 85) return 'ممتاز';
  if (rate >= 70) return 'جيد جدًا';
  if (rate >= 50) return 'جيد';
  if (rate >= 30) return 'مقبول';
  return 'ضعيف';
}

export function Dashboard() {
  const [data, setData] = useState<DashboardResponse | null>(null);
  const [usingMock, setUsingMock] = useState(false);
  const [showEntryModal, setShowEntryModal] = useState(false);
  const [applyingTop, setApplyingTop] = useState(false);
  const [openRecsTotal, setOpenRecsTotal] = useState<number | null>(null);
  const session = getSession();

  function loadDashboard() {
    fetchDashboard({ organizationId: requireOrganizationId() })
      .then((d) => { setData(d); setUsingMock(false); })
      .catch(() => {
        // في حال تعذّر الوصول للـ backend (مثلًا أثناء المعاينة بدون تشغيل السيرفر)
        // نعرض بيانات تجريبية بنفس الشكل حتى تبقى الواجهة قابلة للمعاينة الفورية.
        setData(mockDashboard);
        setUsingMock(true);
      });
    fetchRecommendations(requireBranchId(), 'OPEN')
      .then((recs) => setOpenRecsTotal(recs.reduce((sum, r) => sum + r.expectedValue, 0)))
      .catch(() => setOpenRecsTotal(null));
  }

  useEffect(() => {
    loadDashboard();
  }, []);

  async function applyTopRecommendations() {
    if (!data) return;
    const targets = data.topRecommendations.filter((r) => r.priority === 'HIGH').slice(0, 3);
    if (targets.length === 0) return;
    setApplyingTop(true);
    try {
      await Promise.all(targets.map((r) => applyRecommendation(r.id)));
      loadDashboard();
    } finally {
      setApplyingTop(false);
    }
  }

  if (!data) {
    return (
      <div className="app-shell">
        <Sidebar />
        <main className="main-area">
          <p style={{ color: 'var(--text-secondary)' }}>جاري تحميل البيانات...</p>
        </main>
      </div>
    );
  }

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-area">
        <div className="topbar">
          <div style={{ display: 'flex', gap: 10 }}>
            <div className="pill">
              📅 {todayLabel()}
            </div>
            <div className="pill">🏬 {session?.organizationName ?? 'جميع الفروع'}</div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
            <button className="btn-primary" onClick={() => setShowEntryModal(true)}>+ إدخال بيانات اليوم</button>
            <NotificationBell />
            <div className="user-chip">
              <div style={{ textAlign: 'right' }}>
                <div className="name">مرحبًا، {session?.name ?? ''}</div>
                <div className="role">أنت في لوحة القيادة الرئيسية</div>
              </div>
              <div style={{
                width: 38, height: 38, borderRadius: '50%',
                background: 'var(--accent-purple-bg)', display: 'flex',
                alignItems: 'center', justifyContent: 'center', fontSize: 18,
              }}>👤</div>
            </div>
          </div>
        </div>

        {usingMock && (
          <div style={{
            marginBottom: 14, fontSize: 12, color: 'var(--accent-amber)',
            background: 'var(--accent-amber-bg)', padding: '8px 14px', borderRadius: 8,
          }}>
            ⚠️ يتم عرض بيانات تجريبية — تعذّر الاتصال بالـ backend على http://localhost:8080. شغّل الـ backend لعرض بيانات حقيقية.
          </div>
        )}

        <div className="section-title">ملخص الأداء اليومي</div>
        <div className="grid-row grid-4">
          <KpiCard
            icon="%" iconBg="var(--accent-green-bg)"
            label="معدل التوفير من فرص الشراء الجماعي"
            value={`${data.dailyPerformanceSummary.groupBuySavingsRatePercent.toFixed(1)}%`}
            caption={`↑ ${Math.round(data.dailyPerformanceSummary.groupBuySavingsAmountThisMonth).toLocaleString('ar')} شيكل هذا الشهر`}
            captionColor="var(--accent-green)"
          />
          <KpiCard
            icon="🔄" iconBg="var(--accent-blue-bg)"
            label="معدل تدوير مخاطر ركود الأصناف"
            value={`${Math.round(data.dailyPerformanceSummary.inventoryTurnoverRatePercent)}%`}
            caption={turnoverLabel(data.dailyPerformanceSummary.inventoryTurnoverRatePercent)}
            captionColor="var(--accent-blue)"
          />
          <KpiCard
            icon="🛒" iconBg="var(--accent-purple-bg)"
            label="حجم المشتريات المطلوب للأصناف التي يتطلب توفرها"
            value={Math.round(data.dailyPerformanceSummary.purchaseVolumeNeeded).toLocaleString('ar')}
            unit="شيكل"
            caption="بتكلفة شراء أقل"
          />
          <KpiCard
            icon="🏷️" iconBg="var(--accent-amber-bg)"
            label="حجم المبيعات للأصناف المطلوب التخلص منها"
            value={Math.round(data.dailyPerformanceSummary.clearanceVolumeNeeded).toLocaleString('ar')}
            unit="شيكل"
            caption="لتسريع دورانها"
          />
        </div>

        <div className="grid-metrics">
          <MetricCard label="المبيعات اليوم" value={data.salesToday.toLocaleString('ar')} unit="شيكل"
            deltaLabel="عن أمس" deltaValue={data.salesChangePercent} icon="🛒" iconBg="var(--accent-green-bg)" />
          <MetricCard label="إجمالي الربح" value={data.totalProfit.toLocaleString('ar')} unit="شيكل"
            deltaLabel="عن أمس" deltaValue={data.profitChangePercent} icon="📊" iconBg="var(--accent-blue-bg)" />
          <MetricCard label="هامش الربح" value={`${data.marginPercent.toFixed(1)}%`}
            deltaLabel="عن أمس" deltaValue={data.marginChangePercent} icon="%" iconBg="var(--accent-purple-bg)" />
          <MetricCard label="السيولة المتاحة" value={data.availableLiquidity.toLocaleString('ar')} unit="شيكل"
            deltaLabel="عن أمس" deltaValue={data.liquidityChangePercent} icon="💼" iconBg="var(--accent-amber-bg)" />
          <div className="card metric-card">
            <div className="icon" style={{ background: 'var(--accent-green-bg)' }}>📶</div>
            <div className="label">صحة الأعمال</div>
            <HealthGauge score={data.healthScore.totalScore} label={data.healthScore.label} />
          </div>
        </div>

        <div className="grid-row grid-2-1">
          <HealthRadar score={data.healthScore} />
          <SalesChart data={data.salesTrend} />
          <RecommendationsList items={data.topRecommendations} onChanged={loadDashboard} />
        </div>

        <div className="grid-row grid-3">
          <DonutBreakdown
            title="المخزون"
            totalLabel="إجمالي المخزون"
            entries={[
              { label: 'سريع الحركة', value: data.inventoryBreakdown.FAST ?? 0, color: 'var(--accent-green)' },
              { label: 'متوسط الحركة', value: data.inventoryBreakdown.MEDIUM ?? 0, color: 'var(--accent-blue)' },
              { label: 'بطيء الحركة', value: data.inventoryBreakdown.SLOW ?? 0, color: 'var(--accent-amber)' },
              { label: 'راكد', value: data.inventoryBreakdown.STAGNANT ?? 0, color: 'var(--accent-red)' },
            ]}
            footerNote={{
              text: `⚠️ مخزون راكد بحاجة للتصرف: ${Math.round(data.inventoryBreakdown.STAGNANT ?? 0).toLocaleString('ar')} شيكل`,
              tone: 'warn',
            }}
          />
          <DonutBreakdown
            title="السيولة"
            totalLabel="رأس المال العامل"
            entries={[
              { label: 'السيولة المتاحة', value: data.liquidityBreakdown.AVAILABLE ?? 0, color: 'var(--accent-green)' },
              { label: 'الذمم المدينة', value: data.liquidityBreakdown.RECEIVABLES ?? 0, color: 'var(--accent-blue)' },
              { label: 'الالتزامات الحالة', value: data.liquidityBreakdown.PAYABLES ?? 0, color: 'var(--accent-red)' },
            ]}
            footerNote={openRecsTotal !== null && openRecsTotal > 0
              ? { text: `📈 تحرير ${Math.round(openRecsTotal).toLocaleString('ar')} شيكل متوقع من تنفيذ التوصيات المفتوحة`, tone: 'good' }
              : undefined}
          />
          <AttentionTable items={data.itemsNeedingAttention} />
        </div>

        {(() => {
          const highPriorityRecs = data.topRecommendations.filter((r) => r.priority === 'HIGH').slice(0, 3);
          const potentialValue = highPriorityRecs.reduce((sum, r) => sum + r.expectedValue, 0);
          if (highPriorityRecs.length === 0) return null;
          return (
            <div className="footer-banner">
              <span style={{ fontSize: 13 }}>
                🤖 إذا نفذت أهم {highPriorityRecs.length} توصيات عالية الأولوية، ستحقق توفير + ربح إضافي قدره{' '}
                <strong style={{ color: 'var(--accent-green)' }}>{Math.round(potentialValue).toLocaleString('ar')} شيكل</strong> خلال 30 يوم
              </span>
              <button className="btn-primary" onClick={applyTopRecommendations} disabled={applyingTop}>
                {applyingTop ? 'جارِ التطبيق...' : '⚡ تطبيق التوصيات الآن'}
              </button>
            </div>
          );
        })()}
      </main>

      {showEntryModal && (
        <DailyEntryModal
          onClose={() => setShowEntryModal(false)}
          onSubmitted={loadDashboard}
        />
      )}
    </div>
  );
}
