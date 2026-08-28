import { useCallback, useEffect, useState } from 'react';
import {
  fetchStockCounts,
  fetchWaste,
  recordStockCount,
  recordWaste,
  type ItemDto,
  type StockCountDto,
  type WasteRecordDto,
} from '../api/client';

/**
 * تسجيل التوالف والجرد الفعلي - مصدرا مؤشرَي نسبة الهدر ودقة الجرد.
 *
 * الاثنان يعدّلان المخزون فعليًا: التالف يخصم الكمية، والجرد يصحّح الدفتري إلى
 * المعدود. لذلك تُنبّه الواجهة إلى ذلك قبل التنفيذ بدل أن يكتشفه المستخدم لاحقًا.
 */

const REASONS: { code: string; labelAr: string }[] = [
  { code: 'EXPIRY', labelAr: 'انتهاء صلاحية' },
  { code: 'DAMAGE', labelAr: 'تلف' },
  { code: 'THEFT', labelAr: 'فقدان/سرقة' },
  { code: 'OTHER', labelAr: 'أخرى' },
];

export function InventoryQualityCard({ branchId, items, onChanged }: {
  branchId: number;
  items: ItemDto[];
  onChanged?: () => void;
}) {
  const [tab, setTab] = useState<'waste' | 'count'>('waste');
  const [waste, setWaste] = useState<WasteRecordDto[]>([]);
  const [counts, setCounts] = useState<StockCountDto[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [wasteDraft, setWasteDraft] = useState({ itemId: '', quantity: '', reason: 'EXPIRY' });
  const [countDraft, setCountDraft] = useState({ itemId: '', countedQuantity: '' });

  const load = useCallback(async () => {
    try {
      const [w, c] = await Promise.all([fetchWaste(branchId), fetchStockCounts(branchId)]);
      setWaste(w);
      setCounts(c);
      setError(null);
    } catch {
      setError('تعذّر تحميل السجلات');
    }
  }, [branchId]);

  useEffect(() => { void load(); }, [load]);

  useEffect(() => {
    if (items.length === 0) return;
    setWasteDraft((d) => (d.itemId ? d : { ...d, itemId: String(items[0].id) }));
    setCountDraft((d) => (d.itemId ? d : { ...d, itemId: String(items[0].id) }));
  }, [items]);

  const selectedForCount = items.find((i) => String(i.id) === countDraft.itemId);

  async function submitWaste() {
    const quantity = Number(wasteDraft.quantity);
    if (!wasteDraft.itemId || !Number.isFinite(quantity) || quantity <= 0) {
      setError('اختر صنفًا وأدخل كمية أكبر من صفر');
      return;
    }
    setBusy(true);
    try {
      await recordWaste({
        branchId, itemId: Number(wasteDraft.itemId), quantity, reason: wasteDraft.reason,
      });
      setWasteDraft({ ...wasteDraft, quantity: '' });
      await load();
      onChanged?.();
      setError(null);
    } catch {
      setError('تعذّر تسجيل التالف');
    } finally {
      setBusy(false);
    }
  }

  async function submitCount() {
    const countedQuantity = Number(countDraft.countedQuantity);
    if (!countDraft.itemId || !Number.isFinite(countedQuantity) || countedQuantity < 0) {
      setError('اختر صنفًا وأدخل الكمية المعدودة');
      return;
    }
    setBusy(true);
    try {
      await recordStockCount({ branchId, itemId: Number(countDraft.itemId), countedQuantity });
      setCountDraft({ ...countDraft, countedQuantity: '' });
      await load();
      onChanged?.();
      setError(null);
    } catch {
      setError('تعذّر تسجيل الجرد');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="card">
      <div className="card-title">جودة المخزون — التوالف والجرد</div>

      <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 12, lineHeight: 1.8 }}>
        مصدر مؤشرَي «نسبة الهدر» و«دقة الجرد» في مؤشر صحة الأعمال. تسجيل التالف يخصم
        الكمية من الصنف، والجرد يصحّح الكمية الدفترية إلى المعدودة.
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 14 }}>
        <button className={tab === 'waste' ? 'btn-primary' : 'btn-ghost'} onClick={() => setTab('waste')}>
          التوالف
        </button>
        <button className={tab === 'count' ? 'btn-primary' : 'btn-ghost'} onClick={() => setTab('count')}>
          الجرد الفعلي
        </button>
      </div>

      {tab === 'waste' ? (
        <>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center', marginBottom: 14 }}>
            <select value={wasteDraft.itemId} onChange={(e) => setWasteDraft({ ...wasteDraft, itemId: e.target.value })} style={inputStyle}>
              {items.map((i) => <option key={i.id} value={i.id}>{i.name}</option>)}
            </select>
            <input
              type="number" min="0" placeholder="الكمية التالفة"
              value={wasteDraft.quantity}
              onChange={(e) => setWasteDraft({ ...wasteDraft, quantity: e.target.value })}
              style={{ ...inputStyle, width: 130 }}
            />
            <select value={wasteDraft.reason} onChange={(e) => setWasteDraft({ ...wasteDraft, reason: e.target.value })} style={inputStyle}>
              {REASONS.map((r) => <option key={r.code} value={r.code}>{r.labelAr}</option>)}
            </select>
            <button className="btn-primary" disabled={busy} onClick={submitWaste}>تسجيل تالف</button>
          </div>

          <div style={{ overflowX: 'auto' }}>
            <table className="data-table" style={{ minWidth: 480 }}>
              <thead>
                <tr><th>الصنف</th><th>التاريخ</th><th>الكمية</th><th>التكلفة</th><th>السبب</th></tr>
              </thead>
              <tbody>
                {waste.length === 0 && (
                  <tr><td colSpan={5} style={{ color: 'var(--text-secondary)', fontSize: 13 }}>لا توجد توالف مسجّلة</td></tr>
                )}
                {waste.map((w) => (
                  <tr key={w.id}>
                    <td>{w.itemName}</td>
                    <td>{w.wasteDate}</td>
                    <td>{w.quantity.toLocaleString('ar')}</td>
                    <td>{w.totalCost.toLocaleString('ar')}</td>
                    <td>{REASONS.find((r) => r.code === w.reason)?.labelAr ?? w.reason}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      ) : (
        <>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center', marginBottom: 6 }}>
            <select value={countDraft.itemId} onChange={(e) => setCountDraft({ ...countDraft, itemId: e.target.value })} style={inputStyle}>
              {items.map((i) => <option key={i.id} value={i.id}>{i.name}</option>)}
            </select>
            <input
              type="number" min="0" placeholder="الكمية المعدودة"
              value={countDraft.countedQuantity}
              onChange={(e) => setCountDraft({ ...countDraft, countedQuantity: e.target.value })}
              style={{ ...inputStyle, width: 150 }}
            />
            <button className="btn-primary" disabled={busy} onClick={submitCount}>تسجيل جرد</button>
          </div>
          {selectedForCount && (
            <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 14 }}>
              الكمية الدفترية الحالية: <strong>{selectedForCount.quantity.toLocaleString('ar')}</strong>
              {' '}— سيُصحَّح الدفتر إلى ما تُدخله.
            </div>
          )}

          <div style={{ overflowX: 'auto' }}>
            <table className="data-table" style={{ minWidth: 480 }}>
              <thead>
                <tr><th>الصنف</th><th>التاريخ</th><th>الدفتري</th><th>المعدود</th><th>الفارق</th></tr>
              </thead>
              <tbody>
                {counts.length === 0 && (
                  <tr><td colSpan={5} style={{ color: 'var(--text-secondary)', fontSize: 13 }}>لا توجد عمليات جرد مسجّلة</td></tr>
                )}
                {counts.map((c) => (
                  <tr key={c.id}>
                    <td>{c.itemName}</td>
                    <td>{c.countDate}</td>
                    <td>{c.expectedQuantity.toLocaleString('ar')}</td>
                    <td>{c.countedQuantity.toLocaleString('ar')}</td>
                    <td style={{ color: c.discrepancy === 0 ? 'var(--accent-green)' : 'var(--accent-amber)', fontWeight: 700 }}>
                      {c.discrepancy > 0 ? '+' : ''}{c.discrepancy.toLocaleString('ar')}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

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
