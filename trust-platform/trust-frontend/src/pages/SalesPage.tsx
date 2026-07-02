import { useEffect, useState } from 'react';
import { BarChart, Bar, XAxis, YAxis, ResponsiveContainer, Tooltip, CartesianGrid } from 'recharts';
import { Sidebar } from '../components/Sidebar';
import { DailyEntryModal } from '../components/DailyEntryModal';
import { fetchDailyEntries, type DailyEntryDto } from '../api/client';
import { requireBranchId } from '../auth/session';

const RANGE_OPTIONS = [
  { key: 7, label: 'آخر 7 أيام' },
  { key: 30, label: 'آخر 30 يوم' },
  { key: 90, label: 'آخر 90 يوم' },
];

function formatLabel(dateStr: string) {
  const d = new Date(dateStr);
  return `${d.getDate()} ${['ينا', 'فبر', 'مار', 'أبر', 'ماي', 'يون', 'يول', 'أغس', 'سبت', 'أكت', 'نوف', 'ديس'][d.getMonth()]}`;
}

function fmtDate(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function rangeOf(days: number, endOffsetDays: number) {
  const to = new Date();
  to.setDate(to.getDate() - endOffsetDays);
  const from = new Date(to);
  from.setDate(from.getDate() - days);
  return { from: fmtDate(from), to: fmtDate(to) };
}

export function SalesPage() {
  const [rangeDays, setRangeDays] = useState(7);
  const [entries, setEntries] = useState<DailyEntryDto[] | null>(null);
  const [prevEntries, setPrevEntries] = useState<DailyEntryDto[] | null>(null);
  const [editingEntry, setEditingEntry] = useState<DailyEntryDto | null>(null);

  function load() {
    const branchId = requireBranchId();
    const current = rangeOf(rangeDays, 0);
    const previous = rangeOf(rangeDays, rangeDays);
    fetchDailyEntries(branchId, current.from, current.to).then(setEntries);
    fetchDailyEntries(branchId, previous.from, previous.to).then(setPrevEntries);
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [rangeDays]);

  const chartData = entries?.map((e) => ({ label: formatLabel(e.entryDate), sales: e.totalSales })) ?? [];
  const totalSales = entries?.reduce((sum, e) => sum + e.totalSales, 0) ?? 0;
  const prevTotalSales = prevEntries?.reduce((sum, e) => sum + e.totalSales, 0) ?? 0;
  const changePercent = prevTotalSales > 0 ? ((totalSales - prevTotalSales) / prevTotalSales) * 100 : null;

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-area">
        <div className="page-header">
          <div className="page-title">المبيعات</div>
        </div>

        <div className="tabs">
          {RANGE_OPTIONS.map((r) => (
            <div key={r.key} className={`tab ${rangeDays === r.key ? 'active' : ''}`} onClick={() => setRangeDays(r.key)}>
              {r.label}
            </div>
          ))}
        </div>

        <div className="card" style={{ marginBottom: 14 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 4 }}>
            <div className="card-title" style={{ marginBottom: 0 }}>إجمالي المبيعات</div>
            {changePercent !== null && (
              <span className={`delta ${changePercent >= 0 ? 'up' : 'down'}`} style={{ fontSize: 12 }}>
                <span>{changePercent >= 0 ? '▲' : '▼'}</span> {Math.abs(changePercent).toFixed(1)}% عن الفترة السابقة
              </span>
            )}
          </div>
          <div style={{ fontSize: 26, fontWeight: 700, marginBottom: 14 }}>
            {Math.round(totalSales).toLocaleString('ar')} <span style={{ fontSize: 13, color: 'var(--text-secondary)', fontWeight: 400 }}>شيكل</span>
          </div>
          {entries === null ? (
            <p style={{ color: 'var(--text-secondary)' }}>جاري التحميل...</p>
          ) : (
            <ResponsiveContainer width="100%" height={240}>
              <BarChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border-subtle)" vertical={false} />
                <XAxis dataKey="label" tick={{ fill: 'var(--text-secondary)', fontSize: 11 }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fill: 'var(--text-secondary)', fontSize: 11 }} axisLine={false} tickLine={false} />
                <Tooltip
                  contentStyle={{ background: 'var(--bg-panel-alt)', border: '1px solid var(--border-subtle)', borderRadius: 8 }}
                  labelStyle={{ color: 'var(--text-primary)' }}
                />
                <Bar dataKey="sales" fill="var(--accent-blue)" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className="card">
          <div className="card-title">سجل الإدخالات اليومية</div>
          {entries !== null && (
            <table className="attention-table">
              <thead>
                <tr>
                  <th>التاريخ</th>
                  <th>المبيعات</th>
                  <th>التكلفة</th>
                  <th>الربح</th>
                  <th>الهامش</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {[...entries].reverse().map((e) => (
                  <tr key={e.id}>
                    <td>{e.entryDate}</td>
                    <td>{e.totalSales.toLocaleString('ar')}</td>
                    <td>{e.totalCogs.toLocaleString('ar')}</td>
                    <td>{e.totalProfit.toLocaleString('ar')}</td>
                    <td>{e.marginPercent.toFixed(1)}%</td>
                    <td>
                      <button className="btn-secondary" style={{ padding: '4px 12px', fontSize: 12 }} onClick={() => setEditingEntry(e)}>
                        تعديل
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </main>

      {editingEntry && (
        <DailyEntryModal
          initialEntry={editingEntry}
          onClose={() => setEditingEntry(null)}
          onSubmitted={load}
        />
      )}
    </div>
  );
}
