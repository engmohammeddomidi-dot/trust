import { PieChart, Pie, Cell, ResponsiveContainer } from 'recharts';

function bandColor(score: number): string {
  if (score >= 61) return 'var(--accent-green)';
  if (score >= 41) return 'var(--accent-amber)';
  return 'var(--accent-red)';
}

export function HealthGauge({ score, label }: { score: number | null; label: string }) {
  // درجة غير محسوبة تُعرض شرطة لا صفرًا - الصفر رقم مختلق يوحي بأداء سيئ
  const pct = score === null ? 0 : Math.max(0, Math.min(100, score));
  const color = bandColor(pct);
  const data = [{ value: pct }, { value: 100 - pct }];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <div style={{ width: 90, height: 90, position: 'relative' }}>
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={data}
              dataKey="value"
              startAngle={90}
              endAngle={-270}
              innerRadius={32}
              outerRadius={44}
              stroke="none"
              isAnimationActive={false}
            >
              <Cell fill={color} />
              <Cell fill="var(--border-subtle)" />
            </Pie>
          </PieChart>
        </ResponsiveContainer>
        <div
          style={{
            position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column',
            alignItems: 'center', justifyContent: 'center', pointerEvents: 'none',
          }}
        >
          <div style={{ fontSize: 22, fontWeight: 700, lineHeight: 1 }}>{score === null ? '—' : Math.round(pct)}</div>
          <div style={{ fontSize: 10, color: 'var(--text-secondary)' }}>من 100</div>
        </div>
      </div>
      <div className="delta up" style={{ marginTop: 6 }}>{label}</div>
    </div>
  );
}
