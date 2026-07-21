import { useEffect, useState } from 'react';
import { Sidebar } from '../components/Sidebar';
import { Modal } from '../components/Modal';
import {
  createSupplier, fetchMyGroupOrderParticipation, fetchPurchases, fetchSuppliers, updateSupplier,
  type GroupOrderParticipationDto, type PurchaseDto, type SupplierDto,
} from '../api/client';
import { requireBranchId, requireOrganizationId } from '../auth/session';

interface SupplierSummary {
  name: string;
  purchaseCount: number;
  totalSpend: number;
  items: Set<string>;
  lastPurchaseDate: string;
}

function summarize(purchases: PurchaseDto[]): SupplierSummary[] {
  const map = new Map<string, SupplierSummary>();
  for (const p of purchases) {
    const existing = map.get(p.supplierName);
    if (existing) {
      existing.purchaseCount += 1;
      existing.totalSpend += p.totalCost;
      if (p.itemName) existing.items.add(p.itemName);
      if (p.purchaseDate > existing.lastPurchaseDate) existing.lastPurchaseDate = p.purchaseDate;
    } else {
      map.set(p.supplierName, {
        name: p.supplierName,
        purchaseCount: 1,
        totalSpend: p.totalCost,
        items: new Set(p.itemName ? [p.itemName] : []),
        lastPurchaseDate: p.purchaseDate,
      });
    }
  }
  return [...map.values()].sort((a, b) => b.totalSpend - a.totalSpend);
}

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

function ratingClass(rating: number): string {
  if (rating >= 85) return 'status-FAST';
  if (rating >= 70) return 'status-MEDIUM';
  return 'status-STAGNANT';
}

function SupplierFormModal({
  supplier, onClose, onSaved,
}: {
  supplier: SupplierDto | null;
  onClose: () => void;
  onSaved: (s: SupplierDto) => void;
}) {
  const [name, setName] = useState(supplier?.name ?? '');
  const [contactInfo, setContactInfo] = useState(supplier?.contactInfo ?? '');
  const [email, setEmail] = useState(supplier?.email ?? '');
  const [leadTimeDays, setLeadTimeDays] = useState(String(supplier?.leadTimeDays ?? 5));
  const [creditTermsDays, setCreditTermsDays] = useState(String(supplier?.creditTermsDays ?? 0));
  const [rating, setRating] = useState(String(supplier?.rating ?? 80));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSave() {
    if (!name.trim()) {
      setError('اسم المورد مطلوب');
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const payload = {
        name: name.trim(),
        contactInfo: contactInfo.trim() || undefined,
        email: email.trim() || undefined,
        leadTimeDays: Number(leadTimeDays) || 0,
        creditTermsDays: Number(creditTermsDays) || 0,
        rating: Number(rating) || 0,
      };
      const saved = supplier
        ? await updateSupplier(supplier.id, payload)
        : await createSupplier({ organizationId: requireOrganizationId(), ...payload });
      onSaved(saved);
      onClose();
    } catch {
      setError('تعذّر حفظ بيانات المورد. حاول مرة أخرى.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title={supplier ? 'تعديل مورد' : 'مورد جديد'} onClose={onClose}>
      {error && <div className="form-banner-error">{error}</div>}
      <div className="form-group">
        <label>اسم المورد</label>
        <input value={name} onChange={(e) => setName(e.target.value)} />
      </div>
      <div className="form-group">
        <label>معلومات التواصل</label>
        <input value={contactInfo} onChange={(e) => setContactInfo(e.target.value)} placeholder="رقم هاتف" />
      </div>
      <div className="form-group">
        <label>البريد الإلكتروني</label>
        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="لربط حساب بوابة المورد" />
      </div>
      <div className="grid-row grid-2">
        <div className="form-group">
          <label>مدة التوريد (أيام)</label>
          <input type="number" min={0} value={leadTimeDays} onChange={(e) => setLeadTimeDays(e.target.value)} />
        </div>
        <div className="form-group">
          <label>مدة الائتمان (أيام)</label>
          <input type="number" min={0} value={creditTermsDays} onChange={(e) => setCreditTermsDays(e.target.value)} />
        </div>
      </div>
      <div className="form-group">
        <label>التقييم (0-100)</label>
        <input type="number" min={0} max={100} value={rating} onChange={(e) => setRating(e.target.value)} />
      </div>
      <div className="form-actions">
        <button className="btn-secondary" onClick={onClose}>إلغاء</button>
        <button className="btn-primary" onClick={handleSave} disabled={saving}>
          {saving ? 'جارِ الحفظ...' : 'حفظ'}
        </button>
      </div>
    </Modal>
  );
}

export function SuppliersPage() {
  const [suppliers, setSuppliers] = useState<SupplierDto[] | null>(null);
  const [purchases, setPurchases] = useState<PurchaseDto[] | null>(null);
  const [participations, setParticipations] = useState<GroupOrderParticipationDto[] | null>(null);
  const [modalSupplier, setModalSupplier] = useState<SupplierDto | 'new' | null>(null);

  function loadSuppliers() {
    fetchSuppliers(requireOrganizationId()).then(setSuppliers);
  }

  useEffect(() => {
    loadSuppliers();
    fetchPurchases(requireBranchId()).then(setPurchases);
    fetchMyGroupOrderParticipation().then(setParticipations);
  }, []);

  function handleSaved(saved: SupplierDto) {
    setSuppliers((prev) => {
      if (!prev) return [saved];
      const exists = prev.some((s) => s.id === saved.id);
      return exists ? prev.map((s) => (s.id === saved.id ? saved : s)) : [...prev, saved];
    });
  }

  const purchaseHistorySuppliers = purchases ? summarize(purchases) : [];

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-area">
        <div className="page-header">
          <div className="page-title">الموردون</div>
          <button className="btn-primary" onClick={() => setModalSupplier('new')}>+ مورد جديد</button>
        </div>

        <div className="card" style={{ marginBottom: 14 }}>
          <div className="card-title">ملفات الموردين</div>
          <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 14 }}>
            يستخدم محرك قرار الشراء مدة التوريد والتقييم هنا مباشرة لحساب متى وكم تشتري ومن أي مورد.
          </p>
          {suppliers === null && <p style={{ color: 'var(--text-secondary)' }}>جاري التحميل...</p>}
          {suppliers !== null && suppliers.length === 0 && (
            <p style={{ color: 'var(--text-secondary)' }}>لا يوجد موردون مسجّلون بعد — أضف أول مورد لربطه بأصنافك.</p>
          )}
          {suppliers !== null && suppliers.length > 0 && (
            <table className="attention-table">
              <thead>
                <tr>
                  <th>المورد</th>
                  <th>معلومات التواصل</th>
                  <th>البريد الإلكتروني</th>
                  <th>مدة التوريد</th>
                  <th>مدة الائتمان</th>
                  <th>التقييم</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {suppliers.map((s) => (
                  <tr key={s.id}>
                    <td>{s.name}</td>
                    <td>{s.contactInfo ?? '-'}</td>
                    <td>{s.email ?? '-'}</td>
                    <td>{s.leadTimeDays} يوم</td>
                    <td>{s.creditTermsDays} يوم</td>
                    <td><span className={`status-chip ${ratingClass(s.rating)}`}>{s.rating.toFixed(0)}%</span></td>
                    <td>
                      <button className="btn-secondary" style={{ padding: '4px 10px', fontSize: 12 }} onClick={() => setModalSupplier(s)}>
                        تعديل
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="card" style={{ marginBottom: 14 }}>
          <div className="card-title">سجل المشتريات حسب المورد (تاريخي)</div>
          {purchases === null && <p style={{ color: 'var(--text-secondary)' }}>جاري التحميل...</p>}
          {purchases !== null && purchaseHistorySuppliers.length === 0 && (
            <p style={{ color: 'var(--text-secondary)' }}>لا توجد عمليات شراء مسجّلة بعد.</p>
          )}
          {purchaseHistorySuppliers.length > 0 && (
            <table className="attention-table">
              <thead>
                <tr>
                  <th>المورد</th>
                  <th>عدد الطلبات</th>
                  <th>إجمالي المشتريات</th>
                  <th>الأصناف الموردة</th>
                  <th>آخر عملية شراء</th>
                </tr>
              </thead>
              <tbody>
                {purchaseHistorySuppliers.map((s) => (
                  <tr key={s.name}>
                    <td>{s.name}</td>
                    <td>{s.purchaseCount}</td>
                    <td>{Math.round(s.totalSpend).toLocaleString('ar')} شيكل</td>
                    <td>{s.items.size > 0 ? [...s.items].join('، ') : '-'}</td>
                    <td>{s.lastPurchaseDate}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="card">
          <div className="card-title">مشاركاتي في الطلبات الجماعية 🤝</div>
          {participations === null && <p style={{ color: 'var(--text-secondary)' }}>جاري التحميل...</p>}
          {participations !== null && participations.length === 0 && (
            <p style={{ color: 'var(--text-secondary)' }}>
              لا توجد مشاركات بعد — يمكنك الانضمام لطلب شراء جماعي من صفحة المشتريات.
            </p>
          )}
          {participations !== null && participations.length > 0 && (
            <table className="attention-table">
              <thead>
                <tr>
                  <th>الصنف</th>
                  <th>الكمية</th>
                  <th>السعر الفردي</th>
                  <th>السعر بالجملة</th>
                  <th>التوفير المحقق</th>
                  <th>الحالة</th>
                </tr>
              </thead>
              <tbody>
                {participations.map((p, idx) => (
                  <tr key={idx}>
                    <td>{p.itemName}</td>
                    <td>{p.quantity.toLocaleString('ar')}</td>
                    <td>{p.estimatedMarketPrice.toLocaleString('ar')}</td>
                    <td>{p.negotiatedPrice ? p.negotiatedPrice.toLocaleString('ar') : '-'}</td>
                    <td style={{ color: 'var(--accent-green)' }}>
                      {p.savings != null ? `${Math.round(p.savings).toLocaleString('ar')} شيكل` : '-'}
                    </td>
                    <td><span className={`status-chip ${statusClass[p.status]}`}>{statusLabel[p.status]}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </main>

      {modalSupplier !== null && (
        <SupplierFormModal
          supplier={modalSupplier === 'new' ? null : modalSupplier}
          onClose={() => setModalSupplier(null)}
          onSaved={handleSaved}
        />
      )}
    </div>
  );
}
