import { Radar, RadarChart, PolarGrid, PolarAngleAxis, ResponsiveContainer } from 'recharts';
import type { BhiResultDto } from '../api/client';

/**
 * محاور مؤشر صحة الأعمال. المحاور تأتي من الخادم ولا تُكتب هنا بالاسم - فعدد المحاور
 * وأوزانها قابلة للتعديل لكل فئة نشاط من لوحة المشرف دون نشر واجهة جديدة.
 */
export function HealthRadar({ score }: { score: BhiResultDto }) {
  // المحاور بلا بيانات لا تُرسم على الرادار - تظهر في لوحة التفصيل بدلًا من ذلك
  const data = score.axes
    .filter((a) => a.score !== null)
    .map((a) => ({ subject: a.labelAr, value: a.score as number }));
  const hasScore = score.totalScore !== null;

  return (
    <div className="card">
      <div className="card-title">مؤشر صحة الأعمال</div>

      <div style={{ fontSize: 32, fontWeight: 700, marginBottom: 8 }}>
        {hasScore ? Math.round(score.totalScore as number) : '—'}
        <span style={{ fontSize: 16, color: 'var(--text-secondary)' }}>/100</span>
      </div>

      {data.length > 0 ? (
        <ResponsiveContainer width="100%" height={220}>
          <RadarChart data={data} outerRadius={80}>
            <PolarGrid stroke="var(--border-subtle)" />
            <PolarAngleAxis dataKey="subject" tick={{ fill: 'var(--text-secondary)', fontSize: 11 }} />
            <Radar dataKey="value" stroke="var(--brand)" fill="var(--brand)" fillOpacity={0.22} />
          </RadarChart>
        </ResponsiveContainer>
      ) : (
        <div style={{ height: 220, display: 'flex', alignItems: 'center', justifyContent: 'center',
          color: 'var(--text-secondary)', fontSize: 13 }}>
          لا توجد بيانات كافية لحساب المؤشر بعد
        </div>
      )}

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ color: 'var(--text-primary)', fontSize: 13, fontWeight: 600 }}>{score.label}</div>
        <div style={{ color: 'var(--text-secondary)', fontSize: 11 }}>
          {score.availableIndicatorCount} من {score.totalIndicatorCount} مؤشرًا متاحًا
        </div>
      </div>
    </div>
  );
}
