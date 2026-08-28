import { useEffect, useState } from 'react';
import { Sidebar } from '../components/Sidebar';
import { AddUserModal } from '../components/AddUserModal';
import {
  activateUser, deactivateUser, exportTenantData, fetchAuditLog, fetchBranches, fetchGoals, fetchOrganization,
  fetchPolicy, fetchUsers, updateBranch, updateGoals, updateOrganization, updatePolicy,
  type AuditLogDto, type BranchDto, type GoalDto, type GoalType, type OrganizationDto, type PolicyDto, type UserListDto,
} from '../api/client';
import { MonthlyExpensesCard } from '../components/MonthlyExpensesCard';
import { getSession, requireOrganizationId } from '../auth/session';

const goalLabel: Record<GoalType, string> = {
  INCREASE_PROFITABILITY: 'زيادة الربحية',
  IMPROVE_LIQUIDITY: 'تحسين السيولة',
  PREVENT_STOCKOUTS: 'منع نفاد الأصناف',
  REDUCE_STAGNANT_INVENTORY: 'تقليل المخزون الراكد',
  INCREASE_SALES: 'زيادة المبيعات',
  IMPROVE_SUPPLIER_PERFORMANCE: 'تحسين أداء الموردين',
  INCREASE_INVENTORY_TURNOVER: 'زيادة دوران المخزون',
};

const categoryLabel: Record<string, string> = {
  SUPERMARKET: 'سوبرماركت',
  PHARMACY: 'صيدلية',
  RESTAURANT: 'مطعم',
  RETAIL_CLOTHING: 'تجارة ملابس',
  GENERAL_TRADE: 'تجارة عامة',
  COMPANY_OTHER: 'أخرى',
};

const roleLabel: Record<string, string> = {
  OWNER: 'صاحب المؤسسة',
  BRANCH_MANAGER: 'مدير فرع',
  STAFF: 'موظف',
};

export function SettingsPage() {
  const [org, setOrg] = useState<OrganizationDto | null>(null);
  const [branches, setBranches] = useState<BranchDto[] | null>(null);
  const [orgName, setOrgName] = useState('');
  const [equity, setEquity] = useState('');
  const [savingOrg, setSavingOrg] = useState(false);
  const [savedMessage, setSavedMessage] = useState<string | null>(null);
  const [users, setUsers] = useState<UserListDto[] | null>(null);
  const [showAddUserModal, setShowAddUserModal] = useState(false);
  const [auditLog, setAuditLog] = useState<AuditLogDto[] | null>(null);
  const [exporting, setExporting] = useState(false);
  const [policy, setPolicy] = useState<PolicyDto | null>(null);
  const [liquidityRatioPercent, setLiquidityRatioPercent] = useState('');
  const [minSupplierRating, setMinSupplierRating] = useState('');
  const [savingPolicy, setSavingPolicy] = useState(false);
  const [policySaved, setPolicySaved] = useState(false);
  const [goals, setGoals] = useState<GoalDto[] | null>(null);
  const [savingGoals, setSavingGoals] = useState(false);
  const [goalsSaved, setGoalsSaved] = useState(false);
  const isOwner = getSession()?.role === 'OWNER';
  const branchId = getSession()?.branchId ?? null;

  function load() {
    const organizationId = requireOrganizationId();
    fetchOrganization(organizationId).then((o) => { setOrg(o); setOrgName(o.name); setEquity(o.equity != null ? String(o.equity) : ''); });
    fetchBranches(organizationId).then(setBranches);
    fetchUsers().then(setUsers);
    if (isOwner) {
      fetchAuditLog().then(setAuditLog);
      fetchPolicy(organizationId).then((p) => {
        setPolicy(p);
        setLiquidityRatioPercent(String(Math.round(p.maxPurchaseLiquidityRatio * 100)));
        setMinSupplierRating(String(p.minSupplierRating));
      });
      fetchGoals(organizationId).then(setGoals);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function saveOrgName() {
    setSavingOrg(true);
    try {
      const parsedEquity = equity.trim() === '' ? null : Number(equity);
      const updated = await updateOrganization(requireOrganizationId(), orgName, parsedEquity);
      setOrg(updated);
      setEquity(updated.equity != null ? String(updated.equity) : '');
      setSavedMessage('تم حفظ بيانات المؤسسة');
      setTimeout(() => setSavedMessage(null), 2500);
    } finally {
      setSavingOrg(false);
    }
  }

  async function toggleBranchActive(branch: BranchDto) {
    const updated = await updateBranch(branch.id, { name: branch.name, city: branch.city ?? undefined, active: !branch.active });
    setBranches((prev) => prev?.map((b) => (b.id === updated.id ? updated : b)) ?? null);
  }

  async function saveBranchField(branch: BranchDto, field: 'name' | 'city', value: string) {
    const updated = await updateBranch(branch.id, { name: field === 'name' ? value : branch.name, city: field === 'city' ? value : branch.city ?? undefined, active: branch.active });
    setBranches((prev) => prev?.map((b) => (b.id === updated.id ? updated : b)) ?? null);
  }

  async function toggleUserActive(user: UserListDto) {
    const updated = user.active ? await deactivateUser(user.id) : await activateUser(user.id);
    setUsers((prev) => prev?.map((u) => (u.id === updated.id ? updated : u)) ?? null);
  }

  async function savePolicy() {
    const ratio = Number(liquidityRatioPercent) / 100;
    const rating = Number(minSupplierRating);
    if (!(ratio > 0 && ratio <= 1) || rating < 0 || rating > 100) return;
    setSavingPolicy(true);
    try {
      const updated = await updatePolicy(requireOrganizationId(), { maxPurchaseLiquidityRatio: ratio, minSupplierRating: rating });
      setPolicy(updated);
      setPolicySaved(true);
      setTimeout(() => setPolicySaved(false), 2500);
    } finally {
      setSavingPolicy(false);
    }
  }

  function setGoalPriority(type: GoalType, priority: number) {
    setGoals((prev) => prev?.map((g) => (g.type === type ? { ...g, priority } : g)) ?? null);
  }

  async function saveGoals() {
    if (!goals) return;
    setSavingGoals(true);
    try {
      const updated = await updateGoals(requireOrganizationId(), goals);
      setGoals(updated);
      setGoalsSaved(true);
      setTimeout(() => setGoalsSaved(false), 2500);
    } finally {
      setSavingGoals(false);
    }
  }

  async function handleExportData() {
    setExporting(true);
    try {
      const data = await exportTenantData();
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `بيانات-${org?.name ?? 'المؤسسة'}-${new Date().toISOString().slice(0, 10)}.json`;
      link.click();
      URL.revokeObjectURL(url);
    } finally {
      setExporting(false);
    }
  }

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-area">
        <div className="page-header">
          <div className="page-title">الإعدادات</div>
        </div>

        <div className="card" style={{ marginBottom: 14 }}>
          <div className="card-title">بيانات المؤسسة</div>
          {org === null ? (
            <p style={{ color: 'var(--text-secondary)' }}>جاري التحميل...</p>
          ) : (
            <>
              <div className="form-group">
                <label>اسم المؤسسة</label>
                <input value={orgName} onChange={(e) => setOrgName(e.target.value)} />
              </div>
              <div className="form-group">
                <label>تصنيف النشاط</label>
                <input value={categoryLabel[org.category] ?? org.category} disabled />
              </div>
              <div className="form-group">
                <label>حقوق الملكية (شيكل)</label>
                <input
                  type="number" min="0" value={equity} placeholder="اتركه فارغًا إن لم يُحتسب بعد"
                  onChange={(e) => setEquity(e.target.value)}
                />
                <small style={{ color: 'var(--text-secondary)', fontSize: 11 }}>
                  يُستخدم لحساب «نسبة الدين إلى حقوق الملكية» في مؤشر صحة الأعمال.
                </small>
              </div>
              <div className="form-actions" style={{ justifyContent: 'flex-start' }}>
                <button className="btn-primary" onClick={saveOrgName}
                  disabled={savingOrg || (orgName === org.name && equity === (org.equity != null ? String(org.equity) : ''))}>
                  {savingOrg ? 'جارِ الحفظ...' : 'حفظ'}
                </button>
                {savedMessage && <span style={{ color: 'var(--accent-green)', fontSize: 13, alignSelf: 'center' }}>{savedMessage}</span>}
              </div>
            </>
          )}
        </div>

        {branchId !== null && (
          <div style={{ marginBottom: 14 }}>
            <MonthlyExpensesCard branchId={branchId} />
          </div>
        )}

        <div className="card" style={{ marginBottom: 14 }}>
          <div className="card-title">الفروع</div>
          {branches === null && <p style={{ color: 'var(--text-secondary)' }}>جاري التحميل...</p>}
          {branches !== null && (
            <table className="attention-table">
              <thead>
                <tr>
                  <th>اسم الفرع</th>
                  <th>المدينة</th>
                  <th>الحالة</th>
                </tr>
              </thead>
              <tbody>
                {branches.map((b) => (
                  <tr key={b.id}>
                    <td>
                      <input
                        defaultValue={b.name}
                        onBlur={(e) => e.target.value !== b.name && saveBranchField(b, 'name', e.target.value)}
                        style={{
                          background: 'transparent', border: 'none', color: 'var(--text-primary)', fontSize: 13, width: '100%',
                        }}
                      />
                    </td>
                    <td>
                      <input
                        defaultValue={b.city ?? ''}
                        onBlur={(e) => e.target.value !== (b.city ?? '') && saveBranchField(b, 'city', e.target.value)}
                        style={{
                          background: 'transparent', border: 'none', color: 'var(--text-primary)', fontSize: 13, width: '100%',
                        }}
                      />
                    </td>
                    <td>
                      <span
                        className={`status-chip ${b.active ? 'status-FAST' : 'status-STAGNANT'}`}
                        style={{ cursor: 'pointer' }}
                        onClick={() => toggleBranchActive(b)}
                        title="اضغط للتبديل"
                      >
                        {b.active ? 'فعّال' : 'مغلق'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="card" style={{ marginBottom: 14 }}>
          <div className="page-header" style={{ marginBottom: 14 }}>
            <div className="card-title" style={{ marginBottom: 0 }}>فريق العمل</div>
            {isOwner && (
              <button className="btn-primary" onClick={() => setShowAddUserModal(true)}>+ إضافة موظف</button>
            )}
          </div>
          {users === null && <p style={{ color: 'var(--text-secondary)' }}>جاري التحميل...</p>}
          {users !== null && (
            <table className="attention-table">
              <thead>
                <tr>
                  <th>الاسم</th>
                  <th>البريد الإلكتروني</th>
                  <th>الدور</th>
                  <th>الفرع</th>
                  <th>الحالة</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id}>
                    <td>{u.name}</td>
                    <td>{u.email}</td>
                    <td>{roleLabel[u.role] ?? u.role}</td>
                    <td>{u.branchName ?? '-'}</td>
                    <td>
                      <span
                        className={`status-chip ${u.active ? 'status-FAST' : 'status-STAGNANT'}`}
                        style={{ cursor: isOwner && u.role !== 'OWNER' ? 'pointer' : 'default' }}
                        onClick={() => isOwner && u.role !== 'OWNER' && toggleUserActive(u)}
                        title={isOwner && u.role !== 'OWNER' ? 'اضغط للتبديل' : undefined}
                      >
                        {u.active ? 'فعّال' : 'معطّل'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {isOwner && (
          <div className="card" style={{ marginBottom: 14 }}>
            <div className="card-title">سياسات محرك القرار</div>
            <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 14 }}>
              تتحكم هذه القيم بمحرك قرار الشراء بدل ثوابت مبرمجة — يُطبَّقان في كل مرة تُولَّد فيها قرارات جديدة.
            </p>
            {policy === null ? (
              <p style={{ color: 'var(--text-secondary)' }}>جاري التحميل...</p>
            ) : (
              <>
                <div className="grid-row grid-2">
                  <div className="form-group">
                    <label>الحد الأقصى لقيمة طلبية شراء واحدة من السيولة المتاحة (%)</label>
                    <input type="number" min={1} max={100} value={liquidityRatioPercent} onChange={(e) => setLiquidityRatioPercent(e.target.value)} />
                  </div>
                  <div className="form-group">
                    <label>الحد الأدنى لتقييم المورد المقبول (0-100)</label>
                    <input type="number" min={0} max={100} value={minSupplierRating} onChange={(e) => setMinSupplierRating(e.target.value)} />
                  </div>
                </div>
                <div className="form-actions" style={{ justifyContent: 'flex-start' }}>
                  <button className="btn-primary" onClick={savePolicy} disabled={savingPolicy}>
                    {savingPolicy ? 'جارِ الحفظ...' : 'حفظ السياسات'}
                  </button>
                  {policySaved && <span style={{ color: 'var(--accent-green)', fontSize: 13, alignSelf: 'center' }}>تم الحفظ</span>}
                </div>
              </>
            )}
          </div>
        )}

        {isOwner && (
          <div className="card" style={{ marginBottom: 14 }}>
            <div className="card-title">أولويات العمل</div>
            <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 14 }}>
              رتّب أهدافك (1 = أقل أهمية، 5 = أعلى أولوية)، مجمَّعة تحت الركائز الثلاث.
              الأهداف الموسومة «لا يؤثّر بعد» تُحفظ لكنها لا تغيّر سلوك أي محرك حتى الآن —
              نعرض ذلك صراحةً بدل إيهامك بسبع روافع فاعلة.
            </p>
            {goals === null ? (
              <p style={{ color: 'var(--text-secondary)' }}>جاري التحميل...</p>
            ) : (
              <>
                {Array.from(new Set(goals.map((g) => g.pillar))).map((pillar) => (
                  <div key={pillar} style={{ marginBottom: 10 }}>
                    <div style={{ fontSize: 12, fontWeight: 700, color: 'var(--text-secondary)',
                      borderBottom: '1px solid var(--border-subtle)', paddingBottom: 4, marginBottom: 6 }}>
                      {goals.find((g) => g.pillar === pillar)?.pillarLabelAr}
                    </div>
                {goals.filter((g) => g.pillar === pillar).map((g) => (
                  <div key={g.type} className="recommendation-row">
                    <span className="rec-title">
                      {g.labelAr}
                      {!g.influencesEngine && (
                        <span style={{ fontSize: 10, color: 'var(--text-secondary)', marginInlineStart: 8,
                          border: '1px solid var(--border-subtle)', borderRadius: 999, padding: '1px 8px' }}>
                          لا يؤثّر بعد
                        </span>
                      )}
                    </span>
                    <select
                      value={g.priority}
                      onChange={(e) => setGoalPriority(g.type, Number(e.target.value))}
                      style={{
                        background: 'var(--bg-panel-alt)', border: '1px solid var(--border-subtle)',
                        borderRadius: 'var(--radius-md)', padding: '6px 10px', color: 'var(--text-primary)', fontSize: 13,
                      }}
                    >
                      {[1, 2, 3, 4, 5].map((n) => (
                        <option key={n} value={n}>{'★'.repeat(n)}{'☆'.repeat(5 - n)} ({n})</option>
                      ))}
                    </select>
                  </div>
                ))}
                  </div>
                ))}
                <div className="form-actions" style={{ justifyContent: 'flex-start' }}>
                  <button className="btn-primary" onClick={saveGoals} disabled={savingGoals}>
                    {savingGoals ? 'جارِ الحفظ...' : 'حفظ الأولويات'}
                  </button>
                  {goalsSaved && <span style={{ color: 'var(--accent-green)', fontSize: 13, alignSelf: 'center' }}>تم الحفظ</span>}
                </div>
              </>
            )}
          </div>
        )}

        {isOwner && (
          <div className="card">
            <div className="card-title">سجل التدقيق (آخر العمليات)</div>
            {auditLog === null && <p style={{ color: 'var(--text-secondary)' }}>جاري التحميل...</p>}
            {auditLog !== null && auditLog.length === 0 && (
              <p style={{ color: 'var(--text-secondary)' }}>لا توجد عمليات مسجّلة بعد.</p>
            )}
            {auditLog !== null && auditLog.length > 0 && (
              <table className="attention-table">
                <thead>
                  <tr>
                    <th>التاريخ</th>
                    <th>المستخدم</th>
                    <th>العملية</th>
                    <th>التفاصيل</th>
                  </tr>
                </thead>
                <tbody>
                  {auditLog.slice(0, 20).map((log) => (
                    <tr key={log.id}>
                      <td>{new Date(log.createdAt).toLocaleString('ar')}</td>
                      <td>{log.actorEmail}</td>
                      <td>{log.action}</td>
                      <td>{log.details ?? '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}

        {isOwner && (
          <div className="card" style={{ marginTop: 14 }}>
            <div className="card-title">تصدير البيانات</div>
            <p style={{ color: 'var(--text-secondary)', fontSize: 13, marginBottom: 14 }}>
              نزّل نسخة كاملة من بيانات مؤسستك (الفروع، المخزون، المبيعات اليومية، المشتريات، التوصيات) بصيغة JSON.
            </p>
            <button className="btn-secondary" onClick={handleExportData} disabled={exporting}>
              {exporting ? 'جارِ التصدير...' : 'تصدير جميع البيانات'}
            </button>
          </div>
        )}
      </main>

      {showAddUserModal && (
        <AddUserModal
          onClose={() => setShowAddUserModal(false)}
          onCreated={(u) => setUsers((prev) => (prev ? [...prev, u] : [u]))}
        />
      )}
    </div>
  );
}
