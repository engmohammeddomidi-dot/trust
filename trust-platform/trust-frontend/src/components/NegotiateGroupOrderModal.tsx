import { useState, type FormEvent } from 'react';
import { Modal } from './Modal';
import { negotiateGroupOrder, type GroupOrderDto } from '../api/client';

export function NegotiateGroupOrderModal({ order, onClose, onNegotiated }: {
  order: GroupOrderDto; onClose: () => void; onNegotiated: (o: GroupOrderDto) => void;
}) {
  const [price, setPrice] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const priceNum = parseFloat(price);
  const savingsPercent = priceNum > 0 ? ((order.estimatedMarketPrice - priceNum) / order.estimatedMarketPrice) * 100 : null;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!(priceNum > 0)) {
      setError('السعر المتفاوض عليه يجب أن يكون أكبر من صفر');
      return;
    }
    setSubmitting(true);
    try {
      const updated = await negotiateGroupOrder(order.id, priceNum);
      onNegotiated(updated);
      onClose();
    } catch {
      setError('تعذّر حفظ نتيجة التفاوض. حاول مرة أخرى.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title={`تفاوض — ${order.itemName}`} onClose={onClose}>
      <form onSubmit={handleSubmit}>
        {error && <div className="form-banner-error">{error}</div>}

        <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 14 }}>
          الكمية المجمّعة: {order.currentQuantity.toLocaleString('ar')} من {order.targetQuantity.toLocaleString('ar')} —
          السعر الفردي التقديري: {order.estimatedMarketPrice.toLocaleString('ar')} شيكل
        </p>

        <div className="form-group">
          <label>السعر بالجملة بعد التفاوض مع المورد</label>
          <input type="number" step="0.01" value={price} onChange={(e) => setPrice(e.target.value)} autoFocus />
        </div>

        {savingsPercent !== null && (
          <div className="form-live-margin" style={{ color: savingsPercent > 0 ? 'var(--accent-green)' : 'var(--accent-red)' }}>
            نسبة التوفير للمؤسسات المشاركة: {savingsPercent.toFixed(1)}%
          </div>
        )}

        <div className="form-actions">
          <button type="button" className="btn-secondary" onClick={onClose}>إلغاء</button>
          <button type="submit" className="btn-primary" disabled={submitting}>
            {submitting ? 'جارِ الحفظ...' : 'تأكيد التفاوض'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
