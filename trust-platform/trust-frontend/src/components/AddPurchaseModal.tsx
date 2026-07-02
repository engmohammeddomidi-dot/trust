import { useEffect, useState, type FormEvent } from 'react';
import { Modal } from './Modal';
import { createPurchase, fetchItems, type ItemDto, type PurchaseDto } from '../api/client';
import { requireBranchId } from '../auth/session';

function today(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

interface FormState {
  itemId: string;
  supplierName: string;
  quantity: string;
  costPrice: string;
  purchaseDate: string;
}

const initialForm: FormState = { itemId: '', supplierName: '', quantity: '', costPrice: '', purchaseDate: today() };

export function AddPurchaseModal({ onClose, onCreated }: { onClose: () => void; onCreated: (p: PurchaseDto) => void }) {
  const [items, setItems] = useState<ItemDto[]>([]);
  const [form, setForm] = useState<FormState>(initialForm);
  const [errors, setErrors] = useState<Partial<Record<keyof FormState, string>>>({});
  const [apiError, setApiError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    fetchItems(requireBranchId()).then(setItems);
  }, []);

  function update<K extends keyof FormState>(key: K, value: string) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  function validate(): boolean {
    const next: Partial<Record<keyof FormState, string>> = {};
    if (!form.supplierName.trim()) next.supplierName = 'اسم المورد مطلوب';
    if (!(parseFloat(form.quantity) > 0)) next.quantity = 'الكمية يجب أن تكون أكبر من صفر';
    if (!(parseFloat(form.costPrice) > 0)) next.costPrice = 'سعر التكلفة يجب أن يكون أكبر من صفر';
    if (!form.purchaseDate) next.purchaseDate = 'التاريخ مطلوب';
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setApiError(null);
    if (!validate()) return;
    setSubmitting(true);
    try {
      const created = await createPurchase({
        branchId: requireBranchId(),
        itemId: form.itemId ? Number(form.itemId) : undefined,
        supplierName: form.supplierName.trim(),
        quantity: parseFloat(form.quantity),
        costPrice: parseFloat(form.costPrice),
        purchaseDate: form.purchaseDate,
      });
      onCreated(created);
      onClose();
    } catch (err) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setApiError(message || 'تعذّر حفظ عملية الشراء. حاول مرة أخرى.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title="تسجيل عملية شراء" onClose={onClose}>
      <form onSubmit={handleSubmit}>
        {apiError && <div className="form-banner-error">{apiError}</div>}

        <div className="form-group">
          <label>اسم المورد</label>
          <input value={form.supplierName} onChange={(e) => update('supplierName', e.target.value)} />
          {errors.supplierName && <div className="form-error">{errors.supplierName}</div>}
        </div>

        <div className="form-group">
          <label>الصنف (اختياري)</label>
          <select
            value={form.itemId}
            onChange={(e) => update('itemId', e.target.value)}
            style={{
              width: '100%', background: 'var(--bg-panel-alt)', border: '1px solid var(--border-subtle)',
              borderRadius: 'var(--radius-md)', padding: '9px 12px', color: 'var(--text-primary)', fontSize: 14,
            }}
          >
            <option value="">بدون ربط بصنف محدد</option>
            {items.map((i) => <option key={i.id} value={i.id}>{i.name}</option>)}
          </select>
        </div>

        <div className="form-group">
          <label>الكمية</label>
          <input type="number" step="0.01" value={form.quantity} onChange={(e) => update('quantity', e.target.value)} />
          {errors.quantity && <div className="form-error">{errors.quantity}</div>}
        </div>

        <div className="form-group">
          <label>سعر التكلفة للوحدة</label>
          <input type="number" step="0.01" value={form.costPrice} onChange={(e) => update('costPrice', e.target.value)} />
          {errors.costPrice && <div className="form-error">{errors.costPrice}</div>}
        </div>

        <div className="form-group">
          <label>تاريخ الشراء</label>
          <input type="date" value={form.purchaseDate} onChange={(e) => update('purchaseDate', e.target.value)} />
          {errors.purchaseDate && <div className="form-error">{errors.purchaseDate}</div>}
        </div>

        <div className="form-actions">
          <button type="button" className="btn-secondary" onClick={onClose}>إلغاء</button>
          <button type="submit" className="btn-primary" disabled={submitting}>
            {submitting ? 'جارِ الحفظ...' : 'حفظ'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
