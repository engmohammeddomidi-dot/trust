import { useCallback, useEffect, useState } from 'react';
import { SupplierSidebar } from '../../components/SupplierSidebar';
import { KpiCard } from '../../components/KpiCard';
import {
  acceptSupplierOrder,
  fetchSupplierOverview,
  rejectSupplierOrder,
  type SupplierPortalOverviewDto,
  type SupplierPortalPurchaseDto,
} from '../../api/client';

const statusLabel: Record<string, string> = {
  SENT: 'بانتظار التوريد',
  RECEIVED: 'تم الاستلام',
};
const statusClass: Record<string, string> = {
  SENT: 'status-MEDIUM',
  RECEIVED: 'status-FAST',
};

const responseLabel: Record<string, string> = {
  PENDING: 'بانتظار ردّك',
  ACCEPTED: 'قبلتَ التوريد',
  REJECTED: 'اعتذرتَ',
};
const responseColor: Record<string, string> = {
  PENDING: 'var(--accent-amber)',
  ACCEPTED: 'var(--accent-green)',
  REJECTED: 'var(--accent-red)',
};

/**
 * خلية الردّ على أمر شراء. القبول التزام بالتوريد ولا يعني الاستلام - يبقى الأمر
 * "بانتظار التوريد" حتى تؤكّد المؤسسة المشترية استلامها فعليًا.
 */
function OrderResponseCell({ order, onDone }: {
  order: SupplierPortalPurchaseDto;
  onDone: () => void;
}) {
  const [busy, setBusy] = useState(false);
  const [rejecting, setRejecting] = useState(false);
  const [reason, setReason] = useState('');
  const [promised, setPromised] = useState('');
  const [error, setError] = useState<string | null>(null);

  if (order.status === 'RECEIVED') {
    return <span style={{ color: 'var(--text-secondary)', fontSize: 12 }}>—</span>;
  }

  if (order.supplierResponse !== 'PENDING') {
    return (
      <div style={{ fontSize: 12, color: responseColor[order.supplierResponse] }}>
        {responseLabel[order.supplierResponse]}
        {order.supplierPromisedDate && (
          <div style={{ color: 'var(--text-secondary)', fontSize: 11 }}>
            وعد بالتسليم: {order.supplierPromisedDate}
          </div>
        )}
        {order.supplierRejectionReason && (
          <div style={{ color: 'var(--text-secondary)', fontSize: 11 }}>
            {order.supplierRejectionReason}
          </div>
        )}
      </div>
    );
  }

  async function run(fn: () => Promise<unknown>) {
    setBusy(true);
    try {
      await fn();
      onDone();
      setError(null);
    } catch {
      setError('تعذّر إرسال الردّ');
    } finally {
      setBusy(false);
    }
  }

  if (rejecting) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 6, minWidth: 190 }}>
        <input
          placeholder="سبب الاعتذار" value={reason}
          onChange={(e) => setReason(e.target.value)}
          style={cellInput}
        />
        <div style={{ display: 'flex', gap: 6 }}>
          <button className="btn-primary" disabled={busy || !reason.trim()}
            onClick={() => run(() => rejectSupplierOrder(order.id, reason.trim()))}>
            تأكيد الاعتذار
          </button>
          <button className="btn-ghost" disabled={busy} onClick={() => setRejecting(false)}>إلغاء</button>
        </div>
        {error && <span style={{ color: 'var(--accent-red)', fontSize: 11 }}>{error}</span>}
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 6, minWidth: 190 }}>
      <input
        type="date" value={promised}
        onChange={(e) => setPromised(e.target.value)}
        title="تاريخ التسليم الذي تلتزم به (اختياري)"
        style={cellInput}
      />
      <div style={{ display: 'flex', gap: 6 }}>
        <button className="btn-primary" disabled={busy}
          onClick={() => run(() => acceptSupplierOrder(order.id, promised || undefined))}>
          قبول
        </button>
        <button className="btn-ghost" disabled={busy} onClick={() => setRejecting(true)}>اعتذار</button>
      </div>
      {error && <span style={{ color: 'var(--accent-red)', fontSize: 11 }}>{error}</span>}
    </div>
  );
}

const cellInput: React.CSSProperties = {
  background: 'var(--bg-input, var(--bg-card))',
  color: 'var(--text-primary)',
  border: '1px solid var(--border-subtle)',
  borderRadius: 6,
  padding: '4px 8px',
  fontSize: 12,
};

export function SupplierOverviewPage() {
  const [overview, setOverview] = useState<SupplierPortalOverviewDto | null>(null);

  const load = useCallback(() => {
    fetchSupplierOverview().then(setOverview);
  }, []);

  useEffect(() => { load(); }, [load]);

  return (
    <div className="app-shell">
      <SupplierSidebar />
      <main className="main-area">
        <div className="page-header">
          <div className="page-title">نظرة عامة</div>
        </div>

        {overview === null ? (
          <p style={{ color: 'var(--text-secondary)' }}>جاري التحميل...</p>
        ) : overview.organizationsServedCount === 0 ? (
          <div className="card">
            <p style={{ color: 'var(--text-secondary)' }}>
              لا يوجد بعد أي مؤسسة تربط حساب المورد هذا بسجل مورّد لديها. تواصل مع المؤسسات التي تتعامل
              معها لإضافة بريدك الإلكتروني ({overview.supplierName}) في ملف المورد الخاص بك عندهم.
            </p>
          </div>
        ) : (
          <>
            <div className="grid-row grid-4" style={{ marginBottom: 14 }}>
              <KpiCard icon="organizations" iconBg="var(--accent-blue)" label="المؤسسات المتعامل معها"
                value={String(overview.organizationsServedCount)} caption="عبر المنصة" />
              <KpiCard icon="item" iconBg="var(--accent-amber)" label="طلبات بانتظار التوريد"
                value={String(overview.openOrdersCount)} unit="طلب"
                caption={`${Math.round(overview.openOrdersValue).toLocaleString('ar')} شيكل`} captionColor="var(--accent-amber)" />
              <KpiCard icon="success" iconBg="var(--accent-green)" label="طلبات مستلمة"
                value={String(overview.receivedOrdersCount)} unit="طلب"
                caption={`${Math.round(overview.totalReceivedValue).toLocaleString('ar')} شيكل`} captionColor="var(--accent-green)" />
              <KpiCard icon="rating" iconBg="var(--accent-purple)" label="متوسط التقييم"
                value={overview.avgRating !== null ? String(overview.avgRating) : '—'} unit={overview.avgRating !== null ? '/100' : undefined}
                caption="حسب تقييم كل مؤسسة" />
            </div>

            <div className="card">
              <div className="card-title">أحدث الطلبات</div>
              {overview.recentOrders.length === 0 ? (
                <p style={{ color: 'var(--text-secondary)', fontSize: 12 }}>لا توجد طلبات مسجّلة بعد</p>
              ) : (
                <table className="attention-table">
                  <thead>
                    <tr>
                      <th>المؤسسة</th>
                      <th>الفرع</th>
                      <th>الصنف</th>
                      <th>الكمية</th>
                      <th>سعر الوحدة</th>
                      <th>الإجمالي</th>
                      <th>الحالة</th>
                      <th>تاريخ الطلب</th>
                      <th>التسليم المتوقع / تاريخ الاستلام</th>
                      <th>ردّك</th>
                    </tr>
                  </thead>
                  <tbody>
                    {overview.recentOrders.map((o) => (
                      <tr key={o.id}>
                        <td>{o.organizationName}</td>
                        <td>{o.branchName}</td>
                        <td>{o.itemName ?? '-'}</td>
                        <td>{o.quantity.toLocaleString('ar')}</td>
                        <td>{o.costPrice.toLocaleString('ar')} شيكل</td>
                        <td>{Math.round(o.quantity * o.costPrice).toLocaleString('ar')} شيكل</td>
                        <td><span className={`status-chip ${statusClass[o.status]}`}>{statusLabel[o.status]}</span></td>
                        <td>{o.purchaseDate}</td>
                        <td>{o.status === 'RECEIVED' ? (o.receivedDate ?? '-') : o.expectedDeliveryDate}</td>
                        <td><OrderResponseCell order={o} onDone={load} /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </>
        )}
      </main>
    </div>
  );
}
