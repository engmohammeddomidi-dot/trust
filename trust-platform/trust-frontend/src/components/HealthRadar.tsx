import { Radar, RadarChart, PolarGrid, PolarAngleAxis, ResponsiveContainer } from 'recharts';
import type { HealthScoreDto } from '../api/client';

export function HealthRadar({ score }: { score: HealthScoreDto }) {
  const data = [
    { subject: 'المبيعات', value: score.salesScore },
    { subject: 'الربحية', value: score.profitScore },
    { subject: 'التسعير', value: score.pricingScore },
    { subject: 'المشتريات', value: score.purchasesScore },
    { subject: 'المخزون', value: score.inventoryScore },
    { subject: 'السيولة', value: score.liquidityScore },
  ];

  return (
    <div className="card">
      <div className="card-title">مؤشر صحة الأعمال</div>
      <div style={{ fontSize: 32, fontWeight: 700, marginBottom: 8 }}>
        {Math.round(score.totalScore)}<span style={{ fontSize: 16, color: 'var(--text-secondary)' }}>/100</span>
      </div>
      <ResponsiveContainer width="100%" height={220}>
        <RadarChart data={data} outerRadius={80}>
          <PolarGrid stroke="var(--border-subtle)" />
          <PolarAngleAxis dataKey="subject" tick={{ fill: 'var(--text-secondary)', fontSize: 11 }} />
          <Radar dataKey="value" stroke="var(--accent-green)" fill="var(--accent-green)" fillOpacity={0.35} />
        </RadarChart>
      </ResponsiveContainer>
      <div style={{ color: 'var(--accent-green)', fontSize: 13, fontWeight: 600 }}>{score.label}</div>
    </div>
  );
}
