import { useEffect, useState } from 'react';
import { AdminSidebar } from '../../components/AdminSidebar';
import { CreateGroupOrderModal } from '../../components/CreateGroupOrderModal';
import { NegotiateGroupOrderModal } from '../../components/NegotiateGroupOrderModal';
import { distributeGroupOrder, fetchAdminGroupOrders, type GroupOrderDto } from '../../api/client';

const statusLabel: Record<string, string> = {
  COLLECTING: 'تجميع',
  NEGOTIATED: 'تم التفاوض',
  DISTRIBUTED: 'تم التوزيع',
  CANCELLED: 'ملغى',
};
const statusClass: Record<string, string> = {
  COLLECTING: 'status-SLOW',
  NEGOTIATED: 'status-MEDIUM',
  DISTRIBUTED: 'status-FAST',
  CANCELLED: 'status-STAGNANT',
};

export function AdminGroupOrdersPage() {
  const [orders, setOrders] = useState<GroupOrderDto[] | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [negotiating, setNegotiating] = useState<GroupOrderDto | null>(null);

  function load() {
    fetchAdminGroupOrders().then(setOrders);
  }

  useEffect(() => {
    load();
  }, []);

  async function handleDistribute(id: number) {
    await distributeGroupOrder(id);
    load();
  }

  return (
    <div className="app-shell">
      <AdminSidebar />
      <main className="main-area">
        <div className="page-header">
          <div className="page-title">الطلبات الجماعية من الموردين</div>
          <button className="btn-primary" onClick={() => setShowCreateModal(true)}>+ إنشاء طلب جماعي</button>
        </div>

        <div className="card">
          {orders === null && <p style={{ color: 'var(--text-secondary)' }}>جاري التحميل...</p>}
          {orders !== null && orders.length === 0 && (
            <p style={{ color: 'var(--text-secondary)' }}>لا توجد طلبات جماعية بعد.</p>
          )}
          {orders !== null && orders.length > 0 && (
            <table className="attention-table">
              <thead>
                <tr>
                  <th>الصنف</th>
                  <th>الكمية المجمّعة</th>
                  <th>عدد المشاركين</th>
                  <th>السعر الفردي</th>
                  <th>السعر بالجملة</th>
                  <th>الحالة</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {orders.map((o) => (
                  <tr key={o.id}>
                    <td>{o.itemName}</td>
                    <td>{o.currentQuantity.toLocaleString('ar')} / {o.targetQuantity.toLocaleString('ar')}</td>
                    <td>{o.participantCount}</td>
                    <td>{o.estimatedMarketPrice.toLocaleString('ar')}</td>
                    <td>{o.negotiatedPrice ? o.negotiatedPrice.toLocaleString('ar') : '-'}</td>
                    <td><span className={`status-chip ${statusClass[o.status]}`}>{statusLabel[o.status]}</span></td>
                    <td>
                      {o.status === 'COLLECTING' && (
                        <button className="btn-secondary" style={{ padding: '4px 12px', fontSize: 12 }} onClick={() => setNegotiating(o)}>
                          تفاوض
                        </button>
                      )}
                      {o.status === 'NEGOTIATED' && (
                        <button className="btn-primary" style={{ padding: '4px 12px', fontSize: 12 }} onClick={() => handleDistribute(o.id)}>
                          توزيع
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </main>

      {showCreateModal && (
        <CreateGroupOrderModal onClose={() => setShowCreateModal(false)} onCreated={load} />
      )}
      {negotiating && (
        <NegotiateGroupOrderModal order={negotiating} onClose={() => setNegotiating(null)} onNegotiated={load} />
      )}
    </div>
  );
}
