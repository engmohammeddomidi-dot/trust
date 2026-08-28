import { useCallback, useEffect, useState } from 'react';
import {
  deleteExpense,
  fetchExpenseCategories,
  fetchExpenses,
  saveExpense,
  type ExpenseCategoryDto,
  type MonthlyExpenseDto,
} from '../api/client';

/**
 * جدول المصاريف التشغيلية الشهرية.
 *
 * هذا المدخل هو ما يفتح مؤشرَي هامش صافي الربح ونسبة المصاريف في محور الربحية، أثقل
 * محاور مؤشر صحة الأعمال وزنًا. لذلك تشرح البطاقة الأثر صراحةً بدل تركه مجهولًا -
 * صاحب المحل لن يُدخل بيانات لا يعرف لماذا تُطلب منه.
 *
 * الكمية × القيمة تحاكي جدول المصاريف الأصلي (ثلاثة موظفين بألفين لكلٍّ)، فلا يضطر
 * المستخدم لضرب الأرقام ذهنيًا.
 */
export function MonthlyExpensesCard({ branchId }: { branchId: number }) {
  const currentMonth = new Date().toISOString().slice(0, 7) + '-01';

  const [month, setMonth] = useState(currentMonth);
  const [rows, setRows] = useState<MonthlyExpenseDto[]>([]);
  const [categories, setCategories] = useState<ExpenseCategoryDto[]>([]);
  const [draft, setDraft] = useState({ category: '', unitAmount: '', quantity: '1' });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setRows(await fetchExpenses(branchId, month));
      setError(null);
    } catch {
      setError('تعذّر تحميل المصاريف');
    }
  }, [branchId, month]);

  useEffect(() => { void load(); }, [load]);

  useEffect(() => {
    fetchExpenseCategories()
      .then((c) => {
        setCategories(c);
        setDraft((d) => (d.category ? d : { ...d, category: c[0]?.code ?? '' }));
      })
      .catch(() => setError('تعذّر تحميل بنود المصاريف'));
  }, []);

  const total = rows.reduce((sum, r) => sum + r.total, 0);

  async function submit() {
    const unitAmount = Number(draft.unitAmount);
    const quantity = Number(draft.quantity);
    if (!draft.category || !Number.isFinite(unitAmount) || unitAmount < 0 || quantity < 1) {
      setError('أدخل بندًا وقيمة وكمية صحيحة');
      return;
    }
    setBusy(true);
    try {
      await saveExpense({ branchId, month, category: draft.category, unitAmount, quantity });
      setDraft((d) => ({ ...d, unitAmount: '', quantity: '1' }));
      await load();
      setError(null);
    } catch {
      setError('تعذّر حفظ البند');
    } finally {
      setBusy(false);
    }
  }

  async function remove(id: number) {
    setBusy(true);
    try {
      await deleteExpense(id, branchId);
      await load();
    } catch {
      setError('تعذّر حذف البند');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="card">
      <div className="card-title">المصاريف التشغيلية الشهرية</div>

      <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 12, lineHeight: 1.8 }}>
        تُستخدم لحساب هامش صافي الربح ونسبة المصاريف في مؤشر صحة الأعمال. بدونها يبقى
        هذان المؤشران «غير متاح» ويُحتسب محور الربحية بمؤشر واحد فقط.
      </div>

      <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 12, flexWrap: 'wrap' }}>
        <label style={{ fontSize: 12, color: 'var(--text-secondary)' }}>الشهر</label>
        <input
          type="month"
          value={month.slice(0, 7)}
          onChange={(e) => setMonth(e.target.value + '-01')}
          style={inputStyle}
        />
      </div>

      <div style={{ overflowX: 'auto' }}>
        <table className="data-table" style={{ minWidth: 520 }}>
          <thead>
            <tr>
              <th>البند</th>
              <th>القيمة</th>
              <th>العدد</th>
              <th>الإجمالي</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 && (
              <tr><td colSpan={5} style={{ color: 'var(--text-secondary)', fontSize: 13 }}>
                لا توجد مصاريف مسجّلة لهذا الشهر
              </td></tr>
            )}
            {rows.map((r) => (
              <tr key={r.id}>
                <td>{r.categoryLabelAr}</td>
                <td>{r.unitAmount.toLocaleString('ar')}</td>
                <td>{r.quantity}</td>
                <td style={{ fontWeight: 700 }}>{r.total.toLocaleString('ar')}</td>
                <td>
                  <button className="btn-ghost" disabled={busy} onClick={() => r.id && remove(r.id)}>حذف</button>
                </td>
              </tr>
            ))}
            {rows.length > 0 && (
              <tr>
                <td colSpan={3} style={{ fontWeight: 700 }}>الإجمالي الشهري</td>
                <td style={{ fontWeight: 700 }}>{total.toLocaleString('ar')}</td>
                <td />
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div style={{ display: 'flex', gap: 8, marginTop: 12, flexWrap: 'wrap', alignItems: 'center' }}>
        <select
          value={draft.category}
          onChange={(e) => setDraft({ ...draft, category: e.target.value })}
          style={inputStyle}
        >
          {categories.map((c) => <option key={c.code} value={c.code}>{c.labelAr}</option>)}
        </select>
        <input
          type="number" min="0" placeholder="القيمة"
          value={draft.unitAmount}
          onChange={(e) => setDraft({ ...draft, unitAmount: e.target.value })}
          style={{ ...inputStyle, width: 110 }}
        />
        <input
          type="number" min="1" placeholder="العدد"
          value={draft.quantity}
          onChange={(e) => setDraft({ ...draft, quantity: e.target.value })}
          style={{ ...inputStyle, width: 90 }}
        />
        <button className="btn-primary" disabled={busy} onClick={submit}>حفظ البند</button>
      </div>

      {error && <div style={{ color: 'var(--accent-red)', fontSize: 12, marginTop: 8 }}>{error}</div>}
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
