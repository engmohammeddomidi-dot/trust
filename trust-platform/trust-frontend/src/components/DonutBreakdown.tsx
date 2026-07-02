import { PieChart, Pie, Cell, ResponsiveContainer } from 'recharts';

interface BreakdownEntry {
  label: string;
  value: number;
  color: string;
}

export function DonutBreakdown({
  title,
  totalLabel,
  entries,
  footerNote,
}: {
  title: string;
  totalLabel: string;
  entries: BreakdownEntry[];
  footerNote?: { text: string; tone: 'warn' | 'good' };
}) {
  const total = entries.reduce((sum, e) => sum + e.value, 0);

  return (
    <div className="card">
      <div className="card-title">{title}</div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        <div style={{ width: 130, height: 130, position: 'relative', flexShrink: 0 }}>
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie data={entries} dataKey="value" innerRadius={40} outerRadius={62} paddingAngle={2}>
                {entries.map((e) => (
                  <Cell key={e.label} fill={e.color} stroke="none" />
                ))}
              </Pie>
            </PieChart>
          </ResponsiveContainer>
          <div style={{
            position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column',
            alignItems: 'center', justifyContent: 'center', pointerEvents: 'none',
          }}>
            <div style={{ fontSize: 10, color: 'var(--text-secondary)' }}>{totalLabel}</div>
            <div style={{ fontSize: 15, fontWeight: 700 }}>{Math.round(total).toLocaleString('ar')}</div>
          </div>
        </div>
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 8 }}>
          {entries.map((e) => (
            <div key={e.label} style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12 }}>
              <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <span style={{ width: 8, height: 8, borderRadius: '50%', background: e.color, display: 'inline-block' }} />
                {e.label}
              </span>
              <span style={{ fontWeight: 600 }}>{Math.round(e.value).toLocaleString('ar')}</span>
            </div>
          ))}
        </div>
      </div>
      {footerNote && (
        <div
          style={{
            marginTop: 14,
            fontSize: 12,
            padding: '8px 12px',
            borderRadius: 8,
            background: footerNote.tone === 'warn' ? 'var(--accent-red-bg)' : 'var(--accent-green-bg)',
            color: footerNote.tone === 'warn' ? 'var(--accent-red)' : 'var(--accent-green)',
          }}
        >
          {footerNote.text}
        </div>
      )}
    </div>
  );
}
