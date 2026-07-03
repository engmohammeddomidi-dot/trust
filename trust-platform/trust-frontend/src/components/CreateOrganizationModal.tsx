import { useState, type FormEvent } from 'react';
import { Modal } from './Modal';
import { createOrganization, type CreateOrganizationResponse } from '../api/client';

const categoryOptions = [
  { value: 'SUPERMARKET', label: 'سوبرماركت' },
  { value: 'PHARMACY', label: 'صيدلية' },
  { value: 'RESTAURANT', label: 'مطعم' },
  { value: 'RETAIL_CLOTHING', label: 'تجارة ملابس' },
  { value: 'GENERAL_TRADE', label: 'تجارة عامة' },
  { value: 'COMPANY_OTHER', label: 'أخرى' },
];

export function CreateOrganizationModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [organizationName, setOrganizationName] = useState('');
  const [category, setCategory] = useState('SUPERMARKET');
  const [branchName, setBranchName] = useState('الفرع الرئيسي');
  const [branchCity, setBranchCity] = useState('');
  const [ownerName, setOwnerName] = useState('');
  const [ownerEmail, setOwnerEmail] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<CreateOrganizationResponse | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!organizationName.trim() || !branchName.trim() || !ownerName.trim() || !ownerEmail.trim()) {
      setError('جميع الحقول الأساسية مطلوبة');
      return;
    }
    setSubmitting(true);
    try {
      const res = await createOrganization({
        organizationName: organizationName.trim(),
        category,
        branchName: branchName.trim(),
        branchCity: branchCity.trim() || undefined,
        ownerName: ownerName.trim(),
        ownerEmail: ownerEmail.trim(),
      });
      setResult(res);
      onCreated();
    } catch (err) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(message || 'تعذّر إنشاء المؤسسة. حاول مرة أخرى.');
    } finally {
      setSubmitting(false);
    }
  }

  if (result) {
    return (
      <Modal title="تم إنشاء المؤسسة بنجاح" onClose={onClose}>
        <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 14 }}>
          شارك بيانات الدخول التالية مع صاحب المؤسسة — لن تظهر كلمة المرور مرة أخرى (لا توجد خدمة بريد فعلية بعد لإرسالها تلقائيًا).
        </p>
        <div style={{
          background: 'var(--bg-panel-alt)', border: '1px solid var(--border-subtle)', borderRadius: 'var(--radius-md)',
          padding: 14, marginBottom: 14, fontSize: 13,
        }}>
          <div style={{ marginBottom: 8 }}><strong>البريد الإلكتروني:</strong> {result.ownerEmail}</div>
          <div><strong>كلمة المرور المؤقتة:</strong> <span style={{ color: 'var(--accent-green)', fontFamily: 'monospace' }}>{result.temporaryPassword}</span></div>
        </div>
        <div className="form-actions">
          <button className="btn-primary" onClick={onClose}>إغلاق</button>
        </div>
      </Modal>
    );
  }

  return (
    <Modal title="إنشاء مؤسسة جديدة" onClose={onClose}>
      <form onSubmit={handleSubmit}>
        {error && <div className="form-banner-error">{error}</div>}

        <div className="form-group">
          <label>اسم المؤسسة</label>
          <input value={organizationName} onChange={(e) => setOrganizationName(e.target.value)} />
        </div>

        <div className="form-group">
          <label>تصنيف النشاط</label>
          <select
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            style={{
              width: '100%', background: 'var(--bg-panel-alt)', border: '1px solid var(--border-subtle)',
              borderRadius: 'var(--radius-md)', padding: '9px 12px', color: 'var(--text-primary)', fontSize: 14,
            }}
          >
            {categoryOptions.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
          </select>
        </div>

        <div className="form-group">
          <label>اسم الفرع الأول</label>
          <input value={branchName} onChange={(e) => setBranchName(e.target.value)} />
        </div>

        <div className="form-group">
          <label>المدينة (اختياري)</label>
          <input value={branchCity} onChange={(e) => setBranchCity(e.target.value)} />
        </div>

        <div className="form-group">
          <label>اسم صاحب المؤسسة</label>
          <input value={ownerName} onChange={(e) => setOwnerName(e.target.value)} />
        </div>

        <div className="form-group">
          <label>البريد الإلكتروني لصاحب المؤسسة</label>
          <input type="email" value={ownerEmail} onChange={(e) => setOwnerEmail(e.target.value)} />
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
