import { useState } from 'react';
import type { BhiAxisScore, BhiIndicatorScore, BhiResultDto } from '../api/client';

/**
 * تفصيل مؤشر صحة الأعمال: المحاور الخمسة وتحت كل محور مؤشراته، مع شرح مقروء لكل رقم
 * وسبب صريح لكل مؤشر غير متاح.
 *
 * هذا هو الفرق العملي بين رقم مبهم ورقم قابل للتفسير: لا يظهر أي درجة دون أن يظهر
 * بجانبها من أين جاءت. والمؤشرات التي تنقصها بيانات تُعرض ولا تُخفى، حتى يرى صاحب
 * المحل ما الذي سيجعل التقييم أدق لو أدخله.
 */

function bandColor(band: BhiIndicatorScore['band']): string {
  switch (band) {
    case 'EXCELLENT': return 'var(--accent-green)';
    case 'GOOD': return 'var(--accent-blue)';
    case 'ACCEPTABLE': return 'var(--accent-amber)';
    case 'WEAK': return 'var(--accent-red)';
    default: return 'var(--text-secondary)';
  }
}

function axisColor(score: number | null): string {
  if (score === null) return 'var(--text-secondary)';
  if (score >= 70) return 'var(--accent-green)';
  if (score >= 55) return 'var(--accent-amber)';
  return 'var(--accent-red)';
}

function IndicatorRow({ indicator }: { indicator: BhiIndicatorScore }) {
  const color = bandColor(indicator.band);

  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: 'minmax(0, 1fr) auto auto',
        gap: 12,
        alignItems: 'start',
        padding: '10px 0',
        borderTop: '1px solid var(--border-subtle)',
        opacity: indicator.available ? 1 : 0.6,
      }}
    >
      <div style={{ minWidth: 0 }}>
        <div style={{ fontSize: 13, fontWeight: 600 }}>{indicator.labelAr}</div>
        <div style={{ fontSize: 11, color: 'var(--text-secondary)', marginTop: 2, lineHeight: 1.6 }}>
          {indicator.explanation}
        </div>
      </div>

      <span
        style={{
          fontSize: 11, fontWeight: 600, color, border: `1px solid ${color}`,
          borderRadius: 999, padding: '2px 10px', whiteSpace: 'nowrap',
        }}
      >
        {indicator.bandLabelAr}
      </span>

      <div style={{ fontSize: 14, fontWeight: 700, minWidth: 44, textAlign: 'left', color }}>
        {indicator.score !== null ? Math.round(indicator.score) : '—'}
      </div>
    </div>
  );
}

function AxisPanel({ axis }: { axis: BhiAxisScore }) {
  const [open, setOpen] = useState(false);
  const color = axisColor(axis.score);
  const unscored = axis.score === null;

  return (
    <div style={{ borderBottom: '1px solid var(--border-subtle)', padding: '4px 0' }}>
      <button
        type="button"
        onClick={() => setOpen(!open)}
        style={{
          width: '100%', display: 'flex', alignItems: 'center', gap: 12,
          background: 'none', border: 'none', cursor: 'pointer', padding: '10px 0',
          color: 'inherit', font: 'inherit', textAlign: 'right',
        }}
      >
        <span style={{ color: 'var(--text-secondary)', fontSize: 11 }}>{open ? '▾' : '◂'}</span>
        <span style={{ flex: 1, fontSize: 13, fontWeight: 600, opacity: unscored ? 0.7 : 1 }}>{axis.labelAr}</span>
        <span style={{ fontSize: 11, color: 'var(--text-secondary)' }}>
          {axis.score === null ? 'غير مُقيَّم' : `وزن ${Math.round(axis.weight * 100)}%`}
        </span>
        <span style={{ fontSize: 15, fontWeight: 700, color, minWidth: 44, textAlign: 'left' }}>
          {axis.score === null ? '—' : Math.round(axis.score)}
        </span>
      </button>

      {open && (
        <div style={{ paddingBottom: 8 }}>
          {axis.indicators.map((i) => <IndicatorRow key={i.code} indicator={i} />)}
        </div>
      )}
    </div>
  );
}

export function BhiBreakdown({ bhi }: { bhi: BhiResultDto }) {
  const missing = bhi.totalIndicatorCount - bhi.availableIndicatorCount;

  return (
    <div className="card">
      <div className="card-title">تفصيل مؤشر صحة الأعمال</div>

      <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4, lineHeight: 1.7 }}>
        اضغط على أي محور لعرض مؤشراته وسبب درجته.
        {missing > 0 && ` ${missing} مؤشرًا بانتظار بيانات لم تُدخَل بعد — إدخالها يجعل التقييم أدق.`}
      </div>

      {bhi.axes.length === 0 ? (
        <div style={{ padding: '24px 0', textAlign: 'center', color: 'var(--text-secondary)', fontSize: 13 }}>
          لا توجد بيانات كافية لحساب أي محور بعد
        </div>
      ) : (
        bhi.axes.map((a) => <AxisPanel key={a.axis} axis={a} />)
      )}
    </div>
  );
}
