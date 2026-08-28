import { useCallback, useEffect, useState } from 'react';
import { AdminSidebar } from '../../components/AdminSidebar';
import { ThemeToggle } from '../../components/ThemeToggle';
import {
  fetchBhiConfig,
  resetBhiThreshold,
  resetBhiWeight,
  saveBhiThreshold,
  saveBhiWeight,
  type BhiConfigDto,
} from '../../api/client';

/**
 * معايرة نموذج مؤشر صحة الأعمال لكل فئة نشاط.
 *
 * وجود هذه الشاشة هو ما يحوّل النموذج من أرقام مدفونة في الشيفرة إلى نموذج يملكه فريق
 * المنتج: ما يُعدّ «دورانًا ممتازًا» لصيدلية ليس ما يُعدّ كذلك لسوبرماركت. القيم غير
 * المعدّلة تأتي من النموذج المرجعي، والمعدَّلة تُوسم بوضوح ويمكن إرجاعها.
 */

const CATEGORIES = [
  { code: 'SUPERMARKET', labelAr: 'سوبرماركت' },
  { code: 'PHARMACY', labelAr: 'صيدلية' },
  { code: 'RESTAURANT', labelAr: 'مطعم' },
  { code: 'RETAIL_CLOTHING', labelAr: 'ملابس' },
  { code: 'GENERAL_TRADE', labelAr: 'تجارة عامة' },
  { code: 'COMPANY_OTHER', labelAr: 'أخرى' },
];

function unitHint(unit: string): string {
  switch (unit) {
    case 'PERCENT': return 'كسر عشري (0.05 = 5%)';
    case 'DAYS': return 'أيام';
    case 'TIMES_PER_YEAR': return 'مرة/سنة';
    default: return 'نسبة';
  }
}

export function AdminBhiConfigPage() {
  const [category, setCategory] = useState('SUPERMARKET');
  const [config, setConfig] = useState<BhiConfigDto | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [drafts, setDrafts] = useState<Record<string, { weak: string; medium: string; excellent: string }>>({});
  const [weightDrafts, setWeightDrafts] = useState<Record<string, string>>({});

  const load = useCallback(async () => {
    try {
      const data = await fetchBhiConfig(category);
      applyConfig(data);
      setError(null);
    } catch {
      setError('تعذّر تحميل الإعدادات');
    }
  }, [category]);

  function applyConfig(data: BhiConfigDto) {
    setConfig(data);
    setDrafts(Object.fromEntries(data.indicators.map((i) => [i.code, {
      weak: String(i.weak), medium: String(i.medium), excellent: String(i.excellent),
    }])));
    setWeightDrafts(Object.fromEntries(data.axes.map((a) => [a.axis, String(a.weight)])));
  }

  useEffect(() => { void load(); }, [load]);

  async function saveThreshold(code: string) {
    const d = drafts[code];
    const nums = [Number(d.weak), Number(d.medium), Number(d.excellent)];
    if (nums.some((n) => !Number.isFinite(n))) {
      setError('الحدود يجب أن تكون أرقامًا');
      return;
    }
    setBusy(true);
    try {
      applyConfig(await saveBhiThreshold({ category, code, weak: nums[0], medium: nums[1], excellent: nums[2] }));
      setError(null);
    } catch {
      setError('تعذّر حفظ الحدود');
    } finally {
      setBusy(false);
    }
  }

  async function saveWeight(axis: string) {
    const weight = Number(weightDrafts[axis]);
    if (!Number.isFinite(weight) || weight < 0) {
      setError('الوزن يجب أن يكون رقمًا موجبًا');
      return;
    }
    setBusy(true);
    try {
      applyConfig(await saveBhiWeight({ category, axis, weight }));
      setError(null);
    } catch {
      setError('تعذّر حفظ الوزن');
    } finally {
      setBusy(false);
    }
  }

  async function reset(kind: 'threshold' | 'weight', key: string) {
    setBusy(true);
    try {
      applyConfig(kind === 'threshold'
        ? await resetBhiThreshold(key, category)
        : await resetBhiWeight(key, category));
      setError(null);
    } catch {
      setError('تعذّر إرجاع القيمة الافتراضية');
    } finally {
      setBusy(false);
    }
  }

  const weightSum = config?.axisWeightSum ?? 0;
  const weightsBalanced = Math.abs(weightSum - 1) < 0.001;

  return (
    <div className="app-shell">
      <AdminSidebar />
      <main className="main-content">
        <div className="page-header">
          <div>
            <h1>معايرة مؤشر صحة الأعمال</h1>
            <p>حدود المؤشرات الثلاثة عشر وأوزان المحاور الخمسة، لكل فئة نشاط</p>
          </div>
          <ThemeToggle />
        </div>

        <div className="card" style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
            <label style={{ fontSize: 13, fontWeight: 600 }}>فئة النشاط</label>
            <select value={category} onChange={(e) => setCategory(e.target.value)} style={inputStyle}>
              {CATEGORIES.map((c) => <option key={c.code} value={c.code}>{c.labelAr}</option>)}
            </select>
            <span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
              القيم غير المعدَّلة تأتي من النموذج المرجعي المعتمد.
            </span>
          </div>
        </div>

        {error && <div className="card" style={{ color: 'var(--accent-red)', marginBottom: 16 }}>{error}</div>}

        <div className="card" style={{ marginBottom: 16 }}>
          <div className="card-title">أوزان المحاور</div>
          {!weightsBalanced && (
            <div style={{ color: 'var(--accent-amber)', fontSize: 12, marginBottom: 10 }}>
              مجموع الأوزان {weightSum.toFixed(3)} ولا يساوي 1 — المؤشر العام يعيد توزيعها
              نسبيًا، لكن الأرقام ستكون أوضح لو ضبطتها.
            </div>
          )}
          <div style={{ overflowX: 'auto' }}>
            <table className="data-table" style={{ minWidth: 520 }}>
              <thead><tr><th>المحور</th><th>الوزن</th><th>المصدر</th><th /></tr></thead>
              <tbody>
                {config?.axes.map((a) => (
                  <tr key={a.axis}>
                    <td>{a.labelAr}</td>
                    <td>
                      <input
                        type="number" step="0.05" min="0"
                        value={weightDrafts[a.axis] ?? ''}
                        onChange={(e) => setWeightDrafts({ ...weightDrafts, [a.axis]: e.target.value })}
                        style={{ ...inputStyle, width: 90 }}
                      />
                    </td>
                    <td style={{ fontSize: 12, color: a.overridden ? 'var(--accent-blue)' : 'var(--text-secondary)' }}>
                      {a.overridden ? 'معدَّل' : 'افتراضي'}
                    </td>
                    <td style={{ display: 'flex', gap: 6 }}>
                      <button className="btn-primary" disabled={busy} onClick={() => saveWeight(a.axis)}>حفظ</button>
                      {a.overridden && (
                        <button className="btn-ghost" disabled={busy} onClick={() => reset('weight', a.axis)}>إرجاع</button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="card">
          <div className="card-title">حدود المؤشرات</div>
          <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 10, lineHeight: 1.8 }}>
            كل مؤشر يُقاس بثلاثة حدود: ضعيف = 40 درجة، متوسط = 70، ممتاز = 100، وما بينها
            تدرّج خطي. في المؤشرات التي «أقل أفضل» يكون الحد الممتاز هو الأصغر.
          </div>
          <div style={{ overflowX: 'auto' }}>
            <table className="data-table" style={{ minWidth: 760 }}>
              <thead>
                <tr><th>المؤشر</th><th>الاتجاه</th><th>ضعيف</th><th>متوسط</th><th>ممتاز</th><th>المصدر</th><th /></tr>
              </thead>
              <tbody>
                {config?.indicators.map((i) => (
                  <tr key={i.code}>
                    <td>
                      <div>{i.labelAr}</div>
                      <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>{unitHint(i.unit)}</div>
                    </td>
                    <td style={{ fontSize: 12 }}>{i.direction === 'HIGHER_BETTER' ? 'أعلى أفضل' : 'أقل أفضل'}</td>
                    {(['weak', 'medium', 'excellent'] as const).map((field) => (
                      <td key={field}>
                        <input
                          type="number" step="any"
                          value={drafts[i.code]?.[field] ?? ''}
                          onChange={(e) => setDrafts({
                            ...drafts,
                            [i.code]: { ...drafts[i.code], [field]: e.target.value },
                          })}
                          style={{ ...inputStyle, width: 90 }}
                        />
                      </td>
                    ))}
                    <td style={{ fontSize: 12, color: i.overridden ? 'var(--accent-blue)' : 'var(--text-secondary)' }}>
                      {i.overridden ? 'معدَّل' : 'افتراضي'}
                    </td>
                    <td style={{ display: 'flex', gap: 6 }}>
                      <button className="btn-primary" disabled={busy} onClick={() => saveThreshold(i.code)}>حفظ</button>
                      {i.overridden && (
                        <button className="btn-ghost" disabled={busy} onClick={() => reset('threshold', i.code)}>إرجاع</button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>
  );
}

const inputStyle: React.CSSProperties = {
  background: 'var(--bg-input, var(--bg-card))',
  color: 'var(--text-primary)',
  border: '1px solid var(--border-subtle)',
  borderRadius: 8,
  padding: '6px 10px',
  fontSize: 13,
};
