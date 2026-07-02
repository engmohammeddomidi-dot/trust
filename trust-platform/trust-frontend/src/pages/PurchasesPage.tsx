import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Sidebar } from '../components/Sidebar';
import { AddPurchaseModal } from '../components/AddPurchaseModal';
import {
  fetchOpenGroupOrders, fetchPurchases, fetchRecommendations, joinGroupOrder,
  type GroupOrderDto, type PurchaseDto, type RecommendationDto,
} from '../api/client';
import { requireBranchId } from '../auth/session';

export function PurchasesPage() {
  const [purchases, setPurchases] = useState<PurchaseDto[] | null>(null);
  const [relatedRecs, setRelatedRecs] = useState<RecommendationDto[] | null>(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [openOrders, setOpenOrders] = useState<GroupOrderDto[] | null>(null);
  const [joinQuantities, setJoinQuantities] = useState<Record<number, string>>({});
  const [joiningId, setJoiningId] = useState<number | null>(null);
  const [joinError, setJoinError] = useState<string | null>(null);

  function load() {
    const branchId = requireBranchId();
    fetchPurchases(branchId).then(setPurchases);
    fetchRecommendations(branchId, 'OPEN').then((recs) =>
      setRelatedRecs(recs.filter((r) => r.type === 'STOP_PURCHASE' || r.type === 'INCREASE_ORDER'))
    );
    fetchOpenGroupOrders().then(setOpenOrders);
  }

  useEffect(() => {
    load();
  }, []);

  async function handleJoin(orderId: number) {
    const quantity = parseFloat(joinQuantities[orderId] ?? '');
    if (!(quantity > 0)) {
      setJoinError('أدخل كمية صحيحة أكبر من صفر');
      return;
    }
    setJoinError(null);
    setJoiningId(orderId);
    try {
      await joinGroupOrder(orderId, quantity);
      setJoinQuantities((prev) => ({ ...prev, [orderId]: '' }));
      fetchOpenGroupOrders().then(setOpenOrders);
    } catch {
      setJoinError('تعذّر الانضمام للطلب. حاول مرة أخرى.');
    } finally {
      setJoiningId(null);
    }
  }

  const totalSpend = purchases?.reduce((sum, p) => sum + p.totalCost, 0) ?? 0;

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-area">
        <div className="page-header">
          <div className="page-title">المشتريات</div>
          <button className="btn-primary" onClick={() => setShowAddModal(true)}>+ تسجيل شراء</button>
        </div>

        <div className="grid-row grid-2" style={{ marginBottom: 14 }}>
          <div className="card">
            <div className="label">إجمالي المشتريات المسجّلة</div>
            <div className="value" style={{ fontSize: 22, fontWeight: 700 }}>
              {Math.round(totalSpend).toLocaleString('ar')} <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>شيكل</span>
            </div>
          </div>
          <div className="card">
            <div className="label">عدد عمليات الشراء</div>
            <div className="value" style={{ fontSize: 22, fontWeight: 700 }}>{purchases?.length ?? 0}</div>
          </div>
        </div>

        {openOrders !== null && openOrders.length > 0 && (
          <div className="card" style={{ marginBottom: 14 }}>
            <div className="card-title">طلبات شراء جماعي متاحة للانضمام 🤝</div>
            <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 14 }}>
              انضم بكميتك لتجميع طلب أكبر — كلما زادت الكمية المجمّعة زادت فرصة تفاوض المنصة على سعر جملة أفضل.
            </p>
            {joinError && <div className="form-banner-error">{joinError}</div>}
            {openOrders.map((o) => (
              <div key={o.id} className="recommendation-row" style={{ flexWrap: 'wrap', gap: 10 }}>
                <span className="rec-title">
                  {o.itemName} — مجمّع حاليًا {o.currentQuantity.toLocaleString('ar')} من {o.targetQuantity.toLocaleString('ar')}
                  {' '}(سعر فردي: {o.estimatedMarketPrice.toLocaleString('ar')} شيكل)
                </span>
                <input
                  type="number"
                  step="0.01"
                  placeholder="الكمية"
                  value={joinQuantities[o.id] ?? ''}
                  onChange={(e) => setJoinQuantities((prev) => ({ ...prev, [o.id]: e.target.value }))}
                  style={{
                    width: 100, background: 'var(--bg-panel-alt)', border: '1px solid var(--border-subtle)',
                    borderRadius: 'var(--radius-md)', padding: '6px 10px', color: 'var(--text-primary)', fontSize: 13,
                  }}
                />
                <button
                  className="btn-primary"
                  style={{ padding: '6px 14px', fontSize: 12 }}
                  disabled={joiningId === o.id}
                  onClick={() => handleJoin(o.id)}
                >
                  {joiningId === o.id ? 'جارِ الانضمام...' : 'انضمام'}
                </button>
              </div>
            ))}
          </div>
        )}

        {relatedRecs !== null && relatedRecs.length > 0 && (
          <div className="card" style={{ marginBottom: 14 }}>
            <div className="card-title">توصيات مرتبطة بالمشتريات</div>
            {relatedRecs.map((r) => (
              <div className="recommendation-row" key={r.id}>
                <span className={`priority-tag priority-${r.priority}`}>{r.priority === 'HIGH' ? 'عالية' : r.priority === 'MEDIUM' ? 'متوسطة' : 'منخفضة'}</span>
                <span className="rec-title">{r.title}</span>
              </div>
            ))}
            <div style={{ textAlign: 'center', marginTop: 10 }}>
              <Link to="/notifications" style={{ color: 'var(--accent-blue)', fontSize: 13, textDecoration: 'none' }}>
                ← عرض كل التوصيات
              </Link>
            </div>
          </div>
        )}

        <div className="card">
          <div className="card-title">سجل عمليات الشراء</div>
          {purchases === null && <p style={{ color: 'var(--text-secondary)' }}>جاري التحميل...</p>}
          {purchases !== null && purchases.length === 0 && (
            <p style={{ color: 'var(--text-secondary)' }}>لا توجد عمليات شراء مسجّلة بعد.</p>
          )}
          {purchases !== null && purchases.length > 0 && (
            <table className="attention-table">
              <thead>
                <tr>
                  <th>التاريخ</th>
                  <th>المورد</th>
                  <th>الصنف</th>
                  <th>الكمية</th>
                  <th>سعر الوحدة</th>
                  <th>الإجمالي</th>
                </tr>
              </thead>
              <tbody>
                {purchases.map((p) => (
                  <tr key={p.id}>
                    <td>{p.purchaseDate}</td>
                    <td>{p.supplierName}</td>
                    <td>{p.itemName ?? '-'}</td>
                    <td>{p.quantity.toLocaleString('ar')}</td>
                    <td>{p.costPrice.toLocaleString('ar')}</td>
                    <td>{Math.round(p.totalCost).toLocaleString('ar')}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </main>

      {showAddModal && (
        <AddPurchaseModal
          onClose={() => setShowAddModal(false)}
          onCreated={() => load()}
        />
      )}
    </div>
  );
}
