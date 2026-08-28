import { useEffect, useState } from 'react';
import { Sidebar } from '../components/Sidebar';
import { applyRecommendation, dismissRecommendation, fetchRecommendations, regenerateRecommendations, type RecommendationDto } from '../api/client';
import { requireBranchId } from '../auth/session';
import { Icon } from '../components/Icon';

type StatusFilter = 'OPEN' | 'APPLIED' | 'DISMISSED' | 'ALL';

const STATUS_TABS: { key: StatusFilter; label: string }[] = [
  { key: 'OPEN', label: 'مفتوحة' },
  { key: 'APPLIED', label: 'مطبّقة' },
  { key: 'DISMISSED', label: 'متجاهَلة' },
  { key: 'ALL', label: 'الكل' },
];

const priorityLabel: Record<string, string> = { HIGH: 'عالية', MEDIUM: 'متوسطة', LOW: 'منخفضة' };
const typeIcon: Record<string, string> = {
  STOP_PURCHASE: 'item',
  INCREASE_ORDER: 'opportunity',
  ADJUST_PRICE: 'pricing',
  PROMOTION_CAMPAIGN: 'pricing',
  LIQUIDITY_ALERT: 'liquidity',
  EXPIRY_ALERT: '⏰',
};
const statusChipLabel: Record<string, string> = { OPEN: 'مفتوحة', APPLIED: 'مطبّقة', DISMISSED: 'متجاهَلة' };
const statusChipClass: Record<string, string> = { OPEN: 'status-SLOW', APPLIED: 'status-FAST', DISMISSED: 'status-STAGNANT' };

export function NotificationsPage() {
  const [recs, setRecs] = useState<RecommendationDto[] | null>(null);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('OPEN');
  const [busyId, setBusyId] = useState<number | null>(null);
  const [generating, setGenerating] = useState(false);

  function load() {
    fetchRecommendations(requireBranchId(), statusFilter === 'ALL' ? undefined : statusFilter).then(setRecs);
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter]);

  async function handle(action: 'apply' | 'dismiss', id: number) {
    setBusyId(id);
    try {
      if (action === 'apply') await applyRecommendation(id);
      else await dismissRecommendation(id);
      load();
    } finally {
      setBusyId(null);
    }
  }

  async function handleGenerate() {
    setGenerating(true);
    try {
      await regenerateRecommendations(requireBranchId());
      load();
    } finally {
      setGenerating(false);
    }
  }

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-area">
        <div className="page-header">
          <div className="page-title">التنبيهات والتوصيات</div>
          <button className="btn-primary" onClick={handleGenerate} disabled={generating}>
            {generating ? 'جارِ التوليد...' : 'توليد التوصيات الآن'}
          </button>
        </div>

        <div className="tabs">
          {STATUS_TABS.map((t) => (
            <div
              key={t.key}
              className={`tab ${statusFilter === t.key ? 'active' : ''}`}
              onClick={() => setStatusFilter(t.key)}
            >
              {t.label}
            </div>
          ))}
        </div>

        <div className="card">
          {recs === null && <p style={{ color: 'var(--text-secondary)' }}>جاري التحميل...</p>}
          {recs !== null && recs.length === 0 && (
            <p style={{ color: 'var(--text-secondary)' }}>لا توجد توصيات في هذا التصنيف.</p>
          )}
          {recs !== null && recs.map((rec) => (
            <div className="recommendation-row" key={rec.id}>
              <span className={`priority-tag priority-${rec.priority}`}>{priorityLabel[rec.priority]}</span>
              <span className="rec-title">{rec.title}</span>
              <span className={`status-chip ${statusChipClass[rec.status]}`}>{statusChipLabel[rec.status]}</span>
              <span className="rec-value">
                {typeIcon[rec.type] ?? 'info'} {Math.round(rec.expectedValue).toLocaleString('ar')} شيكل
              </span>
              {rec.status === 'OPEN' && (
                <>
                  <button
                    className="rec-action rec-action-apply"
                    title="تطبيق"
                    disabled={busyId === rec.id}
                    onClick={() => handle('apply', rec.id)}
                  ><Icon name="approve" /></button>
                  <button
                    className="rec-action rec-action-dismiss"
                    title="تجاهل"
                    disabled={busyId === rec.id}
                    onClick={() => handle('dismiss', rec.id)}
                  ><Icon name="close" /></button>
                </>
              )}
            </div>
          ))}
        </div>
      </main>
    </div>
  );
}
