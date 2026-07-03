import { useEffect, useState } from 'react';
import { AdminSidebar } from '../../components/AdminSidebar';
import { CreateOrganizationModal } from '../../components/CreateOrganizationModal';
import { fetchAdminOrganizations, type AdminOrganizationDto } from '../../api/client';

const categoryLabel: Record<string, string> = {
  SUPERMARKET: 'سوبرماركت',
  PHARMACY: 'صيدلية',
  RESTAURANT: 'مطعم',
  RETAIL_CLOTHING: 'تجارة ملابس',
  GENERAL_TRADE: 'تجارة عامة',
  COMPANY_OTHER: 'أخرى',
};

function scoreColor(score: number): string {
  if (score >= 61) return 'var(--accent-green)';
  if (score >= 41) return 'var(--accent-amber)';
  return 'var(--accent-red)';
}

export function AdminOrganizationsPage() {
  const [orgs, setOrgs] = useState<AdminOrganizationDto[] | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);

  function load() {
    fetchAdminOrganizations().then(setOrgs);
  }

  useEffect(() => {
    load();
  }, []);

  return (
    <div className="app-shell">
      <AdminSidebar />
      <main className="main-area">
        <div className="page-header">
          <div className="page-title">المؤسسات</div>
          <button className="btn-primary" onClick={() => setShowCreateModal(true)}>+ إنشاء مؤسسة جديدة</button>
        </div>

        <div className="card">
          {orgs === null && <p style={{ color: 'var(--text-secondary)' }}>جاري التحميل...</p>}
          {orgs !== null && (
            <table className="attention-table">
              <thead>
                <tr>
                  <th>المؤسسة</th>
                  <th>التصنيف</th>
                  <th>عدد الفروع</th>
                  <th>صحة الأعمال</th>
                  <th>آخر نشاط</th>
                </tr>
              </thead>
              <tbody>
                {orgs.map((o) => (
                  <tr key={o.id}>
                    <td>{o.name}</td>
                    <td>{categoryLabel[o.category] ?? o.category}</td>
                    <td>{o.branchCount}</td>
                    <td style={{ color: scoreColor(o.avgHealthScore), fontWeight: 700 }}>{o.avgHealthScore}/100</td>
                    <td>{o.lastActivityDate ?? '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </main>

      {showCreateModal && (
        <CreateOrganizationModal onClose={() => setShowCreateModal(false)} onCreated={load} />
      )}
    </div>
  );
}
