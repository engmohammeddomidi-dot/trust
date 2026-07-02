import { useEffect, useState } from 'react';
import { Sidebar } from '../components/Sidebar';
import { fetchBranches, fetchOrganization, updateBranch, updateOrganization, type BranchDto, type OrganizationDto } from '../api/client';
import { requireOrganizationId } from '../auth/session';

const categoryLabel: Record<string, string> = {
  SUPERMARKET: 'سوبرماركت',
  PHARMACY: 'صيدلية',
  RESTAURANT: 'مطعم',
  RETAIL_CLOTHING: 'تجارة ملابس',
  GENERAL_TRADE: 'تجارة عامة',
  COMPANY_OTHER: 'أخرى',
};

export function SettingsPage() {
  const [org, setOrg] = useState<OrganizationDto | null>(null);
  const [branches, setBranches] = useState<BranchDto[] | null>(null);
  const [orgName, setOrgName] = useState('');
  const [savingOrg, setSavingOrg] = useState(false);
  const [savedMessage, setSavedMessage] = useState<string | null>(null);

  function load() {
    const organizationId = requireOrganizationId();
    fetchOrganization(organizationId).then((o) => { setOrg(o); setOrgName(o.name); });
    fetchBranches(organizationId).then(setBranches);
  }

  useEffect(() => {
    load();
  }, []);

  async function saveOrgName() {
    setSavingOrg(true);
    try {
      const updated = await updateOrganization(requireOrganizationId(), orgName);
      setOrg(updated);
      setSavedMessage('تم حفظ اسم المؤسسة');
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
              <div className="form-actions" style={{ justifyContent: 'flex-start' }}>
                <button className="btn-primary" onClick={saveOrgName} disabled={savingOrg || orgName === org.name}>
                  {savingOrg ? 'جارِ الحفظ...' : 'حفظ'}
                </button>
                {savedMessage && <span style={{ color: 'var(--accent-green)', fontSize: 13, alignSelf: 'center' }}>{savedMessage}</span>}
              </div>
            </>
          )}
        </div>

        <div className="card">
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
      </main>
    </div>
  );
}
