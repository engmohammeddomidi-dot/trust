import { useState, type FormEvent } from 'react';
import { Modal } from './Modal';
import { submitDailyEntry, type DailyEntryDto } from '../api/client';
import { requireBranchId } from '../auth/session';

function today(): string {
  const d = new Date();
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

interface FormState {
  entryDate: string;
  totalSales: string;
  totalCogs: string;
  totalProfit: string;
  profitTouched: boolean;
  availableLiquidity: string;
  receivables: string;
  payables: string;
}

function formFromEntry(entry?: DailyEntryDto): FormState {
  if (!entry) {
    return {
      entryDate: today(), totalSales: '', totalCogs: '', totalProfit: '', profitTouched: false,
      availableLiquidity: '', receivables: '', payables: '',
    };
  }
  return {
    entryDate: entry.entryDate,
    totalSales: String(entry.totalSales),
    totalCogs: String(entry.totalCogs),
    totalProfit: String(entry.totalProfit),
    profitTouched: true,
    availableLiquidity: String(entry.availableLiquidity),
    receivables: String(entry.receivables),
    payables: String(entry.payables),
  };
}

export function DailyEntryModal({ onClose, onSubmitted, initialEntry }: { onClose: () => void; onSubmitted: () => void; initialEntry?: DailyEntryDto }) {
  const [form, setForm] = useState<FormState>(() => formFromEntry(initialEntry));
  const [errors, setErrors] = useState<Partial<Record<keyof FormState, string>>>({});
  const [apiError, setApiError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const sales = parseFloat(form.totalSales);
  const cogs = parseFloat(form.totalCogs);
  const computedProfit = !isNaN(sales) && !isNaN(cogs) ? sales - cogs : null;
  const effectiveProfit = form.profitTouched && form.totalProfit !== ''
    ? parseFloat(form.totalProfit)
    : computedProfit;
  const marginPercent = sales > 0 && effectiveProfit !== null && !isNaN(effectiveProfit)
    ? (effectiveProfit / sales) * 100
    : null;

  function update<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  function validate(): boolean {
    const next: Partial<Record<keyof FormState, string>> = {};
    if (!form.entryDate) next.entryDate = 'التاريخ مطلوب';
    if (!(parseFloat(form.totalSales) >= 0)) next.totalSales = 'إجمالي المبيعات مطلوب';
    if (!(parseFloat(form.totalCogs) >= 0)) next.totalCogs = 'تكلفة البضاعة المباعة مطلوبة';
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setApiError(null);
    if (!validate()) return;
    setSubmitting(true);
    try {
      await submitDailyEntry({
        branchId: requireBranchId(),
        entryDate: form.entryDate,
        totalSales: parseFloat(form.totalSales),
        totalCogs: parseFloat(form.totalCogs),
        totalProfit: form.profitTouched && form.totalProfit !== '' ? parseFloat(form.totalProfit) : null,
        availableLiquidity: form.availableLiquidity === '' ? 0 : parseFloat(form.availableLiquidity),
        receivables: form.receivables === '' ? 0 : parseFloat(form.receivables),
        payables: form.payables === '' ? 0 : parseFloat(form.payables),
      });
      onSubmitted();
      onClose();
    } catch (err) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setApiError(message || 'تعذّر حفظ بيانات اليوم. حاول مرة أخرى.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title={initialEntry ? `تعديل إدخال ${initialEntry.entryDate}` : 'إدخال بيانات اليوم'} onClose={onClose}>
      <form onSubmit={handleSubmit}>
        {apiError && <div className="form-banner-error">{apiError}</div>}

        <div className="form-group">
          <label>التاريخ</label>
          <input type="date" value={form.entryDate} disabled={!!initialEntry} onChange={(e) => update('entryDate', e.target.value)} />
          {errors.entryDate && <div className="form-error">{errors.entryDate}</div>}
        </div>

        <div className="form-group">
          <label>إجمالي المبيعات</label>
          <input type="number" step="0.01" value={form.totalSales} onChange={(e) => update('totalSales', e.target.value)} />
          {errors.totalSales && <div className="form-error">{errors.totalSales}</div>}
        </div>

        <div className="form-group">
          <label>إجمالي تكلفة البضاعة المباعة (COGS)</label>
          <input type="number" step="0.01" value={form.totalCogs} onChange={(e) => update('totalCogs', e.target.value)} />
          {errors.totalCogs && <div className="form-error">{errors.totalCogs}</div>}
        </div>

        <div className="form-group">
          <label>إجمالي الربح (محسوب تلقائيًا، قابل للتعديل)</label>
          <input
            type="number"
            step="0.01"
            value={form.profitTouched ? form.totalProfit : (computedProfit !== null ? computedProfit.toFixed(2) : '')}
            onChange={(e) => { update('profitTouched', true); update('totalProfit', e.target.value); }}
          />
        </div>

        {marginPercent !== null && (
          <div className="form-live-margin">هامش الربح: {marginPercent.toFixed(1)}%</div>
        )}

        <div className="form-group">
          <label>السيولة المتاحة (اختياري)</label>
          <input type="number" step="0.01" value={form.availableLiquidity} onChange={(e) => update('availableLiquidity', e.target.value)} />
        </div>

        <div className="form-group">
          <label>الذمم المدينة (اختياري)</label>
          <input type="number" step="0.01" value={form.receivables} onChange={(e) => update('receivables', e.target.value)} />
        </div>

        <div className="form-group">
          <label>الالتزامات الحالة (اختياري)</label>
          <input type="number" step="0.01" value={form.payables} onChange={(e) => update('payables', e.target.value)} />
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
