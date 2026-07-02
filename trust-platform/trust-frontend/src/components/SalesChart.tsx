import { BarChart, Bar, XAxis, YAxis, ResponsiveContainer, Tooltip, CartesianGrid } from 'recharts';

const dayNames = ['أحد', 'اثنين', 'ثلاثاء', 'أربعاء', 'خميس', 'جمعة', 'سبت'];

function formatLabel(dateStr: string) {
  const d = new Date(dateStr);
  return `${d.getDate()} ${['ينا', 'فبر', 'مار', 'أبر', 'ماي', 'يون', 'يول', 'أغس', 'سبت', 'أكت', 'نوف', 'ديس'][d.getMonth()]}`;
}

export function SalesChart({ data }: { data: { date: string; sales: number }[] }) {
  const total = data.reduce((sum, d) => sum + d.sales, 0);
  const chartData = data.map((d) => ({ ...d, label: formatLabel(d.date) }));

  return (
    <div className="card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 4 }}>
        <div className="card-title" style={{ marginBottom: 0 }}>المبيعات</div>
        <span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>آخر 7 أيام</span>
      </div>
      <div style={{ fontSize: 26, fontWeight: 700, marginBottom: 14 }}>
        {total.toLocaleString('ar')} <span style={{ fontSize: 13, color: 'var(--text-secondary)', fontWeight: 400 }}>شيكل</span>
      </div>
      <ResponsiveContainer width="100%" height={220}>
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
    </div>
  );
}
