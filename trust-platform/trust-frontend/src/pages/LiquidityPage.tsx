import { useEffect, useState } from 'react';
import { LineChart, Line, XAxis, YAxis, ResponsiveContainer, Tooltip, CartesianGrid, ReferenceLine } from 'recharts';
import { Sidebar } from '../components/Sidebar';
import { fetchBenchmark, fetchDailyEntries, fetchRecommendations, type BenchmarkDto, type DailyEntryDto, type RecommendationDto } from '../api/client';
import { requireBranchId } from '../auth/session';

function formatLabel(dateStr: string) {
  const d = new Date(dateStr);
  return `${d.getDate()} ${['ينا', 'فبر', 'مار', 'أبر', 'ماي', 'يون', 'يول', 'أغس', 'سبت', 'أكت', 'نوف', 'ديس'][d.getMonth()]}`;
}

function last30DaysRange() {
  const to = new Date();
  const from = new Date();
  from.setDate(from.getDate() - 30);
  const fmt = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  return { from: fmt(from), to: fmt(to) };
}

export function LiquidityPage() {
  const [entries, setEntries] = useState<DailyEntryDto[] | null>(null);
  const [benchmark, setBenchmark] = useState<BenchmarkDto | null>(null);
  const [openRecs, setOpenRecs] = useState<RecommendationDto[] | null>(null);

  useEffect(() => {
    const branchId = requireBranchId();
    const { from, to } = last30DaysRange();
    fetchDailyEntries(branchId, from, to).then(setEntries);
    fetchBenchmark(branchId).then(setBenchmark);
    fetchRecommendations(branchId, 'OPEN').then(setOpenRecs);
  }, []);

  const chartData = entries?.map((e) => ({
    label: formatLabel(e.entryDate),
    ratio: e.payables > 0 ? Math.round((e.availableLiquidity / e.payables) * 100) / 100 : null,
  })) ?? [];

  const latest = entries && entries.length > 0 ? entries[entries.length - 1] : null;
  const currentRatio = latest && latest.payables > 0 ? latest.availableLiquidity / latest.payables : null;
  const potentialRelease = openRecs?.reduce((sum, r) => sum + r.expectedValue, 0) ?? 0;

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-area">
        <div className="page-header">
          <div className="page-title">السيولة</div>
        </div>

        <div className="grid-row grid-3" style={{ marginBottom: 14 }}>
          <div className="card">
            <div className="label">السيولة المتاحة</div>
            <div className="value" style={{ fontSize: 22, fontWeight: 700 }}>
              {latest ? Math.round(latest.availableLiquidity).toLocaleString('ar') : '-'} <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>شيكل</span>
            </div>
          </div>
          <div className="card">
            <div className="label">الذمم المدينة</div>
            <div className="value" style={{ fontSize: 22, fontWeight: 700 }}>
              {latest ? Math.round(latest.receivables).toLocaleString('ar') : '-'} <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>شيكل</span>
            </div>
          </div>
          <div className="card">
            <div className="label">الالتزامات الحالة</div>
            <div className="value" style={{ fontSize: 22, fontWeight: 700 }}>
              {latest ? Math.round(latest.payables).toLocaleString('ar') : '-'} <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>شيكل</span>
            </div>
          </div>
        </div>

        <div className="card" style={{ marginBottom: 14 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 4 }}>
            <div className="card-title" style={{ marginBottom: 0 }}>نسبة التداول عبر الزمن</div>
            {currentRatio !== null && benchmark && (
              <span style={{ fontSize: 12, color: currentRatio < benchmark.liquidityRatioMin ? 'var(--accent-red)' : 'var(--accent-green)' }}>
                الحالية: {currentRatio.toFixed(2)} — النطاق الصحي: {benchmark.liquidityRatioMin.toFixed(1)}–{benchmark.liquidityRatioMax.toFixed(1)}
              </span>
            )}
          </div>
          {entries === null ? (
            <p style={{ color: 'var(--text-secondary)' }}>جاري التحميل...</p>
          ) : (
            <ResponsiveContainer width="100%" height={240}>
              <LineChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border-subtle)" vertical={false} />
                <XAxis dataKey="label" tick={{ fill: 'var(--text-secondary)', fontSize: 11 }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fill: 'var(--text-secondary)', fontSize: 11 }} axisLine={false} tickLine={false} />
                <Tooltip
                  contentStyle={{ background: 'var(--bg-panel-alt)', border: '1px solid var(--border-subtle)', borderRadius: 8 }}
                  labelStyle={{ color: 'var(--text-primary)' }}
                />
                {benchmark && <ReferenceLine y={benchmark.liquidityRatioMin} stroke="var(--accent-red)" strokeDasharray="4 4" />}
                <Line type="monotone" dataKey="ratio" stroke="var(--accent-blue)" strokeWidth={2} dot={{ r: 3 }} />
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>

        {openRecs !== null && openRecs.length > 0 && (
          <div className="footer-banner">
            <span style={{ fontSize: 13 }}>
               تنفيذ التوصيات المفتوحة ({openRecs.length}) قد يحرر حتى{' '}
              <strong style={{ color: 'var(--accent-green)' }}>{Math.round(potentialRelease).toLocaleString('ar')} شيكل</strong> من السيولة
            </span>
          </div>
        )}
      </main>
    </div>
  );
}
