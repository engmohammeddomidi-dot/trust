import { useEffect, useState } from 'react';
import { Sidebar } from '../components/Sidebar';
import { fetchBenchmark, fetchItems, type BenchmarkDto, type ItemDto } from '../api/client';
import { requireBranchId } from '../auth/session';

function estimateMonthlyUnits(item: ItemDto): number {
  const dailyRate = item.movementStatus === 'FAST' ? item.quantity * 0.15
    : item.movementStatus === 'MEDIUM' ? item.quantity * 0.07
    : item.movementStatus === 'SLOW' ? item.quantity * 0.03
    : 0;
  return Math.max(item.movementStatus === 'STAGNANT' ? 0 : 1, dailyRate) * 30;
}

function marginHealth(margin: number, bm: BenchmarkDto): 'good' | 'low' | 'high' {
  if (margin < bm.marginRangeLow) return 'low';
  if (margin > bm.marginRangeHigh) return 'high';
  return 'good';
}

const healthLabel: Record<string, string> = { good: 'ضمن النطاق الصحي', low: 'هامش منخفض', high: 'هامش مرتفع' };
const healthClass: Record<string, string> = { good: 'status-FAST', low: 'status-STAGNANT', high: 'status-SLOW' };

export function PricingPage() {
  const [items, setItems] = useState<ItemDto[] | null>(null);
  const [benchmark, setBenchmark] = useState<BenchmarkDto | null>(null);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [simPrice, setSimPrice] = useState('');

  useEffect(() => {
    const branchId = requireBranchId();
    fetchItems(branchId).then(setItems);
    fetchBenchmark(branchId).then(setBenchmark);
  }, []);

  const selected = items?.find((i) => i.id === selectedId) ?? null;
  const simPriceNum = parseFloat(simPrice);
  const simMargin = selected && simPriceNum > 0 ? ((simPriceNum - selected.costPrice) / simPriceNum) * 100 : null;
  const simImpact = selected && simMargin !== null
    ? estimateMonthlyUnits(selected) * (simPriceNum - selected.salePrice)
    : null;

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-area">
        <div className="page-header">
          <div className="page-title">التسعير</div>
        </div>

        <div className="grid-row grid-2-1" style={{ marginBottom: 14 }}>
          <div className="card" style={{ gridColumn: 'span 2' }}>
            <div className="card-title">هامش الربح لكل صنف</div>
            {items === null || benchmark === null ? (
              <p style={{ color: 'var(--text-secondary)' }}>جاري التحميل...</p>
            ) : (
              <table className="attention-table">
                <thead>
                  <tr>
                    <th>الاسم</th>
                    <th>سعر التكلفة</th>
                    <th>سعر البيع</th>
                    <th>هامش الربح</th>
                    <th>الحالة</th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((item) => {
                    const health = marginHealth(item.marginPercent, benchmark);
                    return (
                      <tr key={item.id}>
                        <td>{item.name}</td>
                        <td>{item.costPrice.toLocaleString('ar')}</td>
                        <td>{item.salePrice.toLocaleString('ar')}</td>
                        <td>{item.marginPercent.toFixed(1)}%</td>
                        <td><span className={`status-chip ${healthClass[health]}`}>{healthLabel[health]}</span></td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>

          <div className="card">
            <div className="card-title">محاكاة تغيير السعر</div>
            {items && (
              <>
                <div className="form-group">
                  <label>الصنف</label>
                  <select
                    value={selectedId ?? ''}
                    onChange={(e) => setSelectedId(e.target.value ? Number(e.target.value) : null)}
                    style={{
                      width: '100%', background: 'var(--bg-panel-alt)', border: '1px solid var(--border-subtle)',
                      borderRadius: 'var(--radius-md)', padding: '9px 12px', color: 'var(--text-primary)', fontSize: 14,
                    }}
                  >
                    <option value="">اختر صنفًا</option>
                    {items.map((i) => <option key={i.id} value={i.id}>{i.name}</option>)}
                  </select>
                </div>
                {selected && (
                  <>
                    <div className="form-group">
                      <label>السعر الحالي: {selected.salePrice.toLocaleString('ar')} شيكل (هامش {selected.marginPercent.toFixed(1)}%)</label>
                      <input type="number" step="0.01" placeholder="السعر الجديد المقترح" value={simPrice} onChange={(e) => setSimPrice(e.target.value)} />
                    </div>
                    {simMargin !== null && benchmark && (
                      <div className="form-live-margin" style={{ color: marginHealth(simMargin, benchmark) === 'good' ? 'var(--accent-green)' : 'var(--accent-amber)' }}>
                        الهامش الجديد: {simMargin.toFixed(1)}% — الأثر الشهري المتوقع على الربح: {' '}
                        <strong>{Math.round(simImpact ?? 0).toLocaleString('ar')} شيكل</strong>
                      </div>
                    )}
                  </>
                )}
              </>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}
