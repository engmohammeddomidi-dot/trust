import { useState, type FormEvent } from 'react';
import { Modal } from './Modal';
import { createAdminGroupOrder, type GroupOrderDto } from '../api/client';

export function CreateGroupOrderModal({ onClose, onCreated }: { onClose: () => void; onCreated: (o: GroupOrderDto) => void }) {
  const [itemName, setItemName] = useState('');
  const [targetQuantity, setTargetQuantity] = useState('');
  const [estimatedMarketPrice, setEstimatedMarketPrice] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [apiError, setApiError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function validate(): boolean {
    const next: Record<string, string> = {};
    if (!itemName.trim()) next.itemName = 'اسم الصنف مطلوب';
    if (!(parseFloat(targetQuantity) > 0)) next.targetQuantity = 'الكمية المستهدفة يجب أن تكون أكبر من صفر';
    if (!(parseFloat(estimatedMarketPrice) > 0)) next.estimatedMarketPrice = 'السعر التقديري يجب أن يكون أكبر من صفر';
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setApiError(null);
    if (!validate()) return;
    setSubmitting(true);
    try {
      const created = await createAdminGroupOrder({
        itemName: itemName.trim(),
        targetQuantity: parseFloat(targetQuantity),
        estimatedMarketPrice: parseFloat(estimatedMarketPrice),
      });
      onCreated(created);
      onClose();
    } catch {
      setApiError('تعذّر إنشاء الطلب الجماعي. حاول مرة أخرى.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title="إنشاء طلب شراء جماعي" onClose={onClose}>
      <form onSubmit={handleSubmit}>
        {apiError && <div className="form-banner-error">{apiError}</div>}

        <div className="form-group">
          <label>اسم الصنف</label>
          <input value={itemName} onChange={(e) => setItemName(e.target.value)} />
          {errors.itemName && <div className="form-error">{errors.itemName}</div>}
        </div>

        <div className="form-group">
          <label>الكمية المستهدفة للتجميع</label>
          <input type="number" step="0.01" value={targetQuantity} onChange={(e) => setTargetQuantity(e.target.value)} />
          {errors.targetQuantity && <div className="form-error">{errors.targetQuantity}</div>}
        </div>

        <div className="form-group">
          <label>السعر الفردي التقديري (لو اشترت كل مؤسسة بمفردها)</label>
          <input type="number" step="0.01" value={estimatedMarketPrice} onChange={(e) => setEstimatedMarketPrice(e.target.value)} />
          {errors.estimatedMarketPrice && <div className="form-error">{errors.estimatedMarketPrice}</div>}
        </div>

        <div className="form-actions">
          <button type="button" className="btn-secondary" onClick={onClose}>إلغاء</button>
          <button type="submit" className="btn-primary" disabled={submitting}>
            {submitting ? 'جارِ الإنشاء...' : 'إنشاء'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
