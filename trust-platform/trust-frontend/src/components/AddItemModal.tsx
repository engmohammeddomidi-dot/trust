import { useState, type FormEvent } from 'react';
import { Modal } from './Modal';
import { createItem, type ItemDto } from '../api/client';
import { requireBranchId } from '../auth/session';

interface FormState {
  name: string;
  subCategory: string;
  costPrice: string;
  salePrice: string;
  quantity: string;
  lastSaleDate: string;
  expiryDate: string;
}

const initialForm: FormState = {
  name: '', subCategory: '', costPrice: '', salePrice: '', quantity: '', lastSaleDate: '', expiryDate: '',
};

export function AddItemModal({ onClose, onCreated }: { onClose: () => void; onCreated: (item: ItemDto) => void }) {
  const [form, setForm] = useState<FormState>(initialForm);
  const [errors, setErrors] = useState<Partial<Record<keyof FormState, string>>>({});
  const [apiError, setApiError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const cost = parseFloat(form.costPrice);
  const sale = parseFloat(form.salePrice);
  const marginPercent = sale > 0 && !isNaN(cost) ? ((sale - cost) / sale) * 100 : null;

  function update<K extends keyof FormState>(key: K, value: string) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  function validate(): boolean {
    const next: Partial<Record<keyof FormState, string>> = {};
    if (!form.name.trim()) next.name = 'اسم الصنف مطلوب';
    if (!(parseFloat(form.costPrice) > 0)) next.costPrice = 'سعر التكلفة يجب أن يكون أكبر من صفر';
    if (!(parseFloat(form.salePrice) > 0)) next.salePrice = 'سعر البيع يجب أن يكون أكبر من صفر';
    if (!(parseFloat(form.quantity) > 0)) next.quantity = 'الكمية يجب أن تكون أكبر من صفر';
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setApiError(null);
    if (!validate()) return;
    setSubmitting(true);
    try {
      const created = await createItem({
        branchId: requireBranchId(),
        name: form.name.trim(),
        subCategory: form.subCategory.trim() || undefined,
        costPrice: parseFloat(form.costPrice),
        salePrice: parseFloat(form.salePrice),
        quantity: parseFloat(form.quantity),
        lastSaleDate: form.lastSaleDate || undefined,
        expiryDate: form.expiryDate || undefined,
      });
      onCreated(created);
      onClose();
    } catch (err) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setApiError(message || 'تعذّر حفظ الصنف. حاول مرة أخرى.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title="إضافة صنف" onClose={onClose}>
      <form onSubmit={handleSubmit}>
        {apiError && <div className="form-banner-error">{apiError}</div>}

        <div className="form-group">
          <label>اسم الصنف</label>
          <input value={form.name} onChange={(e) => update('name', e.target.value)} />
          {errors.name && <div className="form-error">{errors.name}</div>}
        </div>

        <div className="form-group">
          <label>الفئة الفرعية (اختياري)</label>
          <input value={form.subCategory} onChange={(e) => update('subCategory', e.target.value)} />
        </div>

        <div className="form-group">
          <label>سعر التكلفة</label>
          <input type="number" step="0.01" value={form.costPrice} onChange={(e) => update('costPrice', e.target.value)} />
          {errors.costPrice && <div className="form-error">{errors.costPrice}</div>}
        </div>

        <div className="form-group">
          <label>سعر البيع</label>
          <input type="number" step="0.01" value={form.salePrice} onChange={(e) => update('salePrice', e.target.value)} />
          {errors.salePrice && <div className="form-error">{errors.salePrice}</div>}
        </div>

        {marginPercent !== null && (
          <div className="form-live-margin">هامش الربح: {marginPercent.toFixed(1)}%</div>
        )}

        <div className="form-group">
          <label>الكمية الحالية</label>
          <input type="number" step="1" value={form.quantity} onChange={(e) => update('quantity', e.target.value)} />
          {errors.quantity && <div className="form-error">{errors.quantity}</div>}
        </div>

        <div className="form-group">
          <label>تاريخ آخر بيع (اختياري)</label>
          <input type="date" value={form.lastSaleDate} onChange={(e) => update('lastSaleDate', e.target.value)} />
        </div>

        <div className="form-group">
          <label>تاريخ انتهاء الصلاحية (اختياري)</label>
          <input type="date" value={form.expiryDate} onChange={(e) => update('expiryDate', e.target.value)} />
        </div>

        <div className="form-actions">
          <button type="button" className="btn-secondary" onClick={onClose}>إلغاء</button>
          <button type="submit" className="btn-primary" disabled={submitting}>
            {submitting ? 'جارِ الحفظ...' : 'حفظ الصنف'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
