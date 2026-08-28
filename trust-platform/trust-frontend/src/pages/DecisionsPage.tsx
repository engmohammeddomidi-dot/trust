import { useEffect, useState } from 'react';
import { Sidebar } from '../components/Sidebar';
import {
  approveDecision, deferDecision, dismissDecision, fetchDecisionQualityScore, fetchDecisions, fetchPurchases,
  fetchSuppliers, modifyDecision, receivePurchase, regenerateDecisions,
  type DecisionDto, type DecisionQualityScoreDto, type PurchaseDto, type SupplierDto,
} from '../api/client';
import { DecisionExplanation } from '../components/DecisionExplanation';
import { requireBranchId, requireOrganizationId } from '../auth/session';
import { Icon } from '../components/Icon';

type Tab = 'OPEN' | 'IN_PROGRESS' | 'HISTORY';

const TABS: { key: Tab; label: string }[] = [
  { key: 'OPEN', label: 'بانتظار قرارك' },
  { key: 'IN_PROGRESS', label: 'معتمدة (بانتظار الاستلام)' },
  { key: 'HISTORY', label: 'السجل' },
];

function confidenceColor(score: number): string {
  if (score >= 80) return 'var(--accent-green)';
  if (score >= 55) return 'var(--accent-yellow, #d4a72c)';
  return 'var(--accent-red)';
}

function ModifyForm({
  decision, suppliers, onCancel, onSaved,
}: {
  decision: DecisionDto;
  suppliers: SupplierDto[] | null;
  onCancel: () => void;
  onSaved: (d: DecisionDto) => void;
}) {
  const [quantity, setQuantity] = useState(String(decision.suggestedQuantity));
  const [supplierId, setSupplierId] = useState(decision.supplierId ? String(decision.supplierId) : '');
  const [saving, setSaving] = useState(false);

  async function handleSave() {
    const q = Number(quantity);
    if (!(q > 0)) return;
    setSaving(true);
    try {
      const updated = await modifyDecision(decision.id, q, supplierId ? Number(supplierId) : undefined);
      onSaved(updated);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div style={{ display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap', marginTop: 10 }}>
      <input
        type="number"
        min={0}
        value={quantity}
        onChange={(e) => setQuantity(e.target.value)}
        style={{
          width: 100, background: 'var(--bg-panel-alt)', border: '1px solid var(--border-subtle)',
          borderRadius: 'var(--radius-md)', padding: '6px 10px', color: 'var(--text-primary)', fontSize: 13,
        }}
      />
      <select
        value={supplierId}
        onChange={(e) => setSupplierId(e.target.value)}
        style={{
          background: 'var(--bg-panel-alt)', border: '1px solid var(--border-subtle)',
          borderRadius: 'var(--radius-md)', padding: '6px 10px', color: 'var(--text-primary)', fontSize: 13,
        }}
      >
        <option value="">— بدون تغيير المورد —</option>
        {suppliers?.map((s) => (
          <option key={s.id} value={s.id}>{s.name}</option>
        ))}
      </select>
      <button className="btn-primary" style={{ padding: '6px 14px', fontSize: 12 }} disabled={saving} onClick={handleSave}>
        {saving ? 'جارِ الحفظ...' : 'حفظ التعديل'}
      </button>
      <button className="btn-secondary" style={{ padding: '6px 14px', fontSize: 12 }} onClick={onCancel}>إلغاء</button>
    </div>
  );
}

function ReceiveForm({
  purchase, onCancel, onSaved,
}: {
  purchase: PurchaseDto;
  onCancel: () => void;
  onSaved: (p: PurchaseDto) => void;
}) {
  const [receivedQuantity, setReceivedQuantity] = useState(String(purchase.quantity));
  const [priceMatched, setPriceMatched] = useState(true);
  const [hasDamage, setHasDamage] = useState(false);
  const [saving, setSaving] = useState(false);

  async function handleSave() {
    const q = Number(receivedQuantity);
    if (!(q >= 0)) return;
    setSaving(true);
    try {
      const updated = await receivePurchase(purchase.id, { receivedQuantity: q, priceMatched, hasDamage });
      onSaved(updated);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div style={{ marginTop: 10, display: 'flex', flexDirection: 'column', gap: 8 }}>
      <div style={{ display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' }}>
        <label style={{ fontSize: 12, color: 'var(--text-secondary)' }}>الكمية المستلمة فعليًا</label>
        <input
          type="number"
          min={0}
          value={receivedQuantity}
          onChange={(e) => setReceivedQuantity(e.target.value)}
          style={{
            width: 100, background: 'var(--bg-panel-alt)', border: '1px solid var(--border-subtle)',
            borderRadius: 'var(--radius-md)', padding: '6px 10px', color: 'var(--text-primary)', fontSize: 13,
          }}
        />
      </div>
      <label style={{ fontSize: 12, display: 'flex', alignItems: 'center', gap: 6 }}>
        <input type="checkbox" checked={priceMatched} onChange={(e) => setPriceMatched(e.target.checked)} />
        السعر مطابق للمتفق عليه
      </label>
      <label style={{ fontSize: 12, display: 'flex', alignItems: 'center', gap: 6 }}>
        <input type="checkbox" checked={hasDamage} onChange={(e) => setHasDamage(e.target.checked)} />
        يوجد تلف في جزء من الشحنة
      </label>
      <div style={{ display: 'flex', gap: 10 }}>
        <button className="btn-primary" style={{ padding: '6px 14px', fontSize: 12 }} disabled={saving} onClick={handleSave}>
          {saving ? 'جارِ التأكيد...' : 'تأكيد الاستلام'}
        </button>
        <button className="btn-secondary" style={{ padding: '6px 14px', fontSize: 12 }} onClick={onCancel}>إلغاء</button>
      </div>
    </div>
  );
}

export function DecisionsPage() {
  const [decisions, setDecisions] = useState<DecisionDto[] | null>(null);
  const [purchases, setPurchases] = useState<PurchaseDto[] | null>(null);
  const [suppliers, setSuppliers] = useState<SupplierDto[] | null>(null);
  const [qualityScore, setQualityScore] = useState<DecisionQualityScoreDto | null>(null);
  const [tab, setTab] = useState<Tab>('OPEN');
  const [busyId, setBusyId] = useState<number | null>(null);
  const [modifyingId, setModifyingId] = useState<number | null>(null);
  const [receivingPurchaseId, setReceivingPurchaseId] = useState<number | null>(null);
  const [regenerating, setRegenerating] = useState(false);

  function load() {
    const branchId = requireBranchId();
    fetchDecisions(branchId).then(setDecisions);
    fetchPurchases(branchId).then(setPurchases);
    fetchSuppliers(requireOrganizationId()).then(setSuppliers);
    fetchDecisionQualityScore(branchId).then(setQualityScore);
  }

  useEffect(() => {
    load();
  }, []);

  async function handleRegenerate() {
    setRegenerating(true);
    try {
      await regenerateDecisions(requireBranchId());
      load();
    } finally {
      setRegenerating(false);
    }
  }

  /**
   * اعتماد بديل يمر بنفس مسار الاعتماد/التعديل، فينشئ أمر شراء حقيقيًا. لو حُدِّثت
   * حالة القرار مباشرةً هنا لكان "اعتماد البديل" يقلب حالة ولا يطلب شيئًا فعلًا.
   */
  async function chooseAlternative(d: DecisionDto, quantity: number) {
    setBusyId(d.id);
    try {
      if (Math.abs(quantity - d.suggestedQuantity) < 0.001) {
        await approveDecision(d.id);
      } else {
        await modifyDecision(d.id, quantity, d.supplierId ?? undefined);
      }
      load();
    } finally {
      setBusyId(null);
    }
  }

  async function handleAction(action: 'approve' | 'defer' | 'dismiss', id: number) {
    setBusyId(id);
    try {
      if (action === 'approve') await approveDecision(id);
      else if (action === 'defer') await deferDecision(id);
      else await dismissDecision(id);
      load();
    } finally {
      setBusyId(null);
    }
  }

  const open = decisions?.filter((d) => d.status === 'OPEN') ?? [];
  const inProgress = decisions?.filter((d) => d.status === 'APPROVED' || d.status === 'MODIFIED') ?? [];
  const history = decisions?.filter((d) => d.status === 'DEFERRED' || d.status === 'DISMISSED') ?? [];
  const purchaseForDecision = (decisionId: number) => purchases?.find((p) => p.decisionId === decisionId) ?? null;

  const visible = tab === 'OPEN' ? open : tab === 'IN_PROGRESS' ? inProgress : history;

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-area">
        <div className="page-header">
          <div className="page-title">قرارات الشراء</div>
          <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
            {qualityScore && qualityScore.qualityScorePercent !== null && (
              <span className="status-chip status-FAST" title="نسبة الطلبيات المستلمة بدون أي انحراف عن المتوقع">
                جودة القرارات: {qualityScore.qualityScorePercent}%
              </span>
            )}
            <button className="btn-primary" onClick={handleRegenerate} disabled={regenerating}>
              {regenerating ? 'جارِ التحليل...' : 'تحديث القرارات'}
            </button>
          </div>
        </div>

        <div className="tabs">
          {TABS.map((t) => (
            <div key={t.key} className={`tab ${tab === t.key ? 'active' : ''}`} onClick={() => setTab(t.key)}>
              {t.label} ({t.key === 'OPEN' ? open.length : t.key === 'IN_PROGRESS' ? inProgress.length : history.length})
            </div>
          ))}
        </div>

        {decisions === null && <p style={{ color: 'var(--text-secondary)' }}>جاري التحميل...</p>}

        {decisions !== null && visible.length === 0 && (
          <div className="card" style={{ textAlign: 'center', padding: '30px 0' }}>
            <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>
              {tab === 'OPEN' ? 'لا توجد قرارات شراء تحتاج مراجعة حاليًا.' : tab === 'IN_PROGRESS' ? 'لا توجد طلبيات معتمدة بانتظار الاستلام.' : 'لا يوجد سجل بعد.'}
            </p>
          </div>
        )}

        {visible.map((d) => {
          const purchase = tab === 'IN_PROGRESS' ? purchaseForDecision(d.id) : null;
          return (
            <div className="card" key={d.id} style={{ marginBottom: 14 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 10 }}>
                <div>
                  <div className="card-title" style={{ marginBottom: 4 }}>{d.itemName}</div>
                  <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
                    الكمية {d.status === 'OPEN' ? 'المقترحة' : 'المعتمدة'}: {(d.approvedQuantity ?? d.suggestedQuantity).toLocaleString('ar')}
                    {d.supplierName && <> — المورد: {d.supplierName}</>}
                  </div>
                </div>
                <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
                  <span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>الأثر المالي</span>
                  <span style={{ fontWeight: 700, color: 'var(--accent-green)' }}>
                    {Math.round(d.financialImpact).toLocaleString('ar')} شيكل
                  </span>
                  <span
                    className="status-chip"
                    style={{ background: 'transparent', border: `1px solid ${confidenceColor(d.confidenceScore)}`, color: confidenceColor(d.confidenceScore) }}
                  >
                    الثقة {d.confidenceScore.toFixed(0)}%
                  </span>
                </div>
              </div>

              <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 10, lineHeight: 1.7 }}>{d.reasonSummary}</p>

              <DecisionExplanation
                decision={d}
                disabled={busyId === d.id}
                onChooseAlternative={tab === 'OPEN' && modifyingId !== d.id
                  ? (q) => chooseAlternative(d, q)
                  : undefined}
              />

              {tab === 'OPEN' && modifyingId !== d.id && (
                <div className="decision-actions">
                  <button className="btn-primary" disabled={busyId === d.id}
                    onClick={() => handleAction('approve', d.id)}>
                    <Icon name="approve" /> اعتماد
                  </button>
                  <button className="btn-secondary" disabled={busyId === d.id}
                    onClick={() => setModifyingId(d.id)}>
                    <Icon name="modify" /> تعديل
                  </button>
                  <button className="btn-secondary" disabled={busyId === d.id}
                    onClick={() => handleAction('defer', d.id)}>
                    <Icon name="defer" /> تأجيل
                  </button>
                  <button className="btn-ghost decision-actions__dismiss" disabled={busyId === d.id}
                    onClick={() => handleAction('dismiss', d.id)}>
                    <Icon name="dismiss" /> تجاهل
                  </button>
                </div>
              )}

              {tab === 'OPEN' && modifyingId === d.id && (
                <ModifyForm
                  decision={d}
                  suppliers={suppliers}
                  onCancel={() => setModifyingId(null)}
                  onSaved={() => { setModifyingId(null); load(); }}
                />
              )}

              {tab === 'IN_PROGRESS' && purchase && (
                <div style={{ marginTop: 10 }}>
                  <span className={`status-chip ${purchase.status === 'RECEIVED' ? 'status-FAST' : 'status-MEDIUM'}`}>
                    {purchase.status === 'RECEIVED' ? 'تم الاستلام' : 'بانتظار الاستلام'}
                  </span>
                  {purchase.status === 'SENT' && receivingPurchaseId !== purchase.id && (
                    <button className="btn-primary" style={{ padding: '6px 14px', fontSize: 12, marginRight: 10 }} onClick={() => setReceivingPurchaseId(purchase.id)}>
                       تسجيل الاستلام
                    </button>
                  )}
                  {purchase.status === 'SENT' && receivingPurchaseId === purchase.id && (
                    <ReceiveForm
                      purchase={purchase}
                      onCancel={() => setReceivingPurchaseId(null)}
                      onSaved={() => { setReceivingPurchaseId(null); load(); }}
                    />
                  )}
                  {purchase.status === 'RECEIVED' && d.actualOutcome && (
                    <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginTop: 8 }}>{d.actualOutcome}</p>
                  )}
                </div>
              )}

              {tab === 'HISTORY' && (
                <span className={`status-chip ${d.status === 'DEFERRED' ? 'status-MEDIUM' : 'status-STAGNANT'}`}>
                  {d.status === 'DEFERRED' ? 'مؤجّل' : 'متجاهَل'}
                </span>
              )}
            </div>
          );
        })}
      </main>
    </div>
  );
}
