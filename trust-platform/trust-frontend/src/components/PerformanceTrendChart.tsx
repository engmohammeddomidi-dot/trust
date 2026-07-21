import { LineChart, Line, XAxis, YAxis, ResponsiveContainer, Tooltip, CartesianGrid } from 'recharts';

function formatLabel(dateStr: string) {
  const d = new Date(dateStr);
  return `${d.getDate()} ${['ينا', 'فبر', 'مار', 'أبر', 'ماي', 'يون', 'يول', 'أغس', 'سبت', 'أكت', 'نوف', 'ديس'][d.getMonth()]}`;
}

export function PerformanceTrendChart({ data }: { data: { date: string; score: number }[] }) {
  return (
    <div className="card">
      <div className="card-title">اتجاه الأداء خلال الأشهر الماضية</div>
      {data.length < 2 ? (
        <p style={{ color: 'var(--text-secondary)', fontSize: 13, padding: '20px 0', textAlign: 'center' }}>
          سيظهر الاتجاه هنا مع تراكم لقطات يومية كافية لمؤشر صحة الأعمال (يُحفظ تلقائيًا كل يوم).
        </p>
      ) : (
        <ResponsiveContainer width="100%" height={220}>
          <LineChart data={data.map((d) => ({ ...d, label: formatLabel(d.date) }))}>
            <CartesianGrid strokeDasharray="3 3" stroke="var(--border-subtle)" vertical={false} />
            <XAxis dataKey="label" tick={{ fill: 'var(--text-secondary)', fontSize: 11 }} axisLine={false} tickLine={false} />
            <YAxis domain={[0, 100]} tick={{ fill: 'var(--text-secondary)', fontSize: 11 }} axisLine={false} tickLine={false} />
            <Tooltip
              contentStyle={{ background: 'var(--bg-panel-alt)', border: '1px solid var(--border-subtle)', borderRadius: 8 }}
              labelStyle={{ color: 'var(--text-primary)' }}
            />
            <Line type="monotone" dataKey="score" stroke="var(--accent-green)" strokeWidth={2} dot={{ r: 3 }} />
          </LineChart>
        </ResponsiveContainer>
      )}
    </div>
  );
}
