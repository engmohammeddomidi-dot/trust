import { useEffect, useState } from 'react';
import { LineChart, Line, XAxis, YAxis, ResponsiveContainer, Tooltip, CartesianGrid } from 'recharts';
import { Sidebar } from '../components/Sidebar';
import { fetchBenchmark, fetchDailyEntries, fetchItems, type BenchmarkDto, type DailyEntryDto, type ItemDto } from '../api/client';
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

export function ProfitabilityPage() {
  const [entries, setEntries] = useState<DailyEntryDto[] | null>(null);
  const [items, setItems] = useState<ItemDto[] | null>(null);
  const [benchmark, setBenchmark] = useState<BenchmarkDto | null>(null);

  useEffect(() => {
    const branchId = requireBranchId();
    const { from, to } = last30DaysRange();
    fetchDailyEntries(branchId, from, to).then(setEntries);
    fetchItems(branchId).then(setItems);
    fetchBenchmark(branchId).then(setBenchmark);
  }, []);

  const chartData = entries?.map((e) => ({ label: formatLabel(e.entryDate), margin: Math.round(e.marginPercent * 10) / 10 })) ?? [];
  const sortedByMargin = items ? [...items].sort((a, b) => b.marginPercent - a.marginPercent) : [];
  const topItems = sortedByMargin.slice(0, 5);
  const bottomItems = sortedByMargin.slice(-5).reverse();
  const avgMargin = entries && entries.length > 0
    ? entries.reduce((sum, e) => sum + e.marginPercent, 0) / entries.length
    : null;

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-area">
        <div className="page-header">
          <div className="page-title">الربحية</div>
        </div>

        <div className="card" style={{ marginBottom: 14 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 4 }}>
            <div className="card-title" style={{ marginBottom: 0 }}>هامش الربح عبر الزمن</div>
            {avgMargin !== null && benchmark && (
              <span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
                المتوسط: {avgMargin.toFixed(1)}% — المرجعي للتصنيف: {benchmark.targetMarginPercent.toFixed(1)}%
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
                <YAxis tick={{ fill: 'var(--text-secondary)', fontSize: 11 }} axisLine={false} tickLine={false} unit="%" />
                <Tooltip
                  contentStyle={{ background: 'var(--bg-panel-alt)', border: '1px solid var(--border-subtle)', borderRadius: 8 }}
                  labelStyle={{ color: 'var(--text-primary)' }}
                />
                <Line type="monotone" dataKey="margin" stroke="var(--accent-purple)" strokeWidth={2} dot={{ r: 3 }} />
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className="grid-row grid-2">
          <div className="card">
            <div className="card-title">الأعلى ربحية</div>
            <table className="attention-table">
              <thead><tr><th>الصنف</th><th>هامش الربح</th></tr></thead>
              <tbody>
                {topItems.map((i) => (
                  <tr key={i.id}><td>{i.name}</td><td style={{ color: 'var(--accent-green)' }}>{i.marginPercent.toFixed(1)}%</td></tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="card">
            <div className="card-title">الأقل ربحية</div>
            <table className="attention-table">
              <thead><tr><th>الصنف</th><th>هامش الربح</th></tr></thead>
              <tbody>
                {bottomItems.map((i) => (
                  <tr key={i.id}><td>{i.name}</td><td style={{ color: 'var(--accent-red)' }}>{i.marginPercent.toFixed(1)}%</td></tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>
  );
}
