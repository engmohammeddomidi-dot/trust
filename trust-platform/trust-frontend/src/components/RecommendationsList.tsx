import { useState } from 'react';
import { Link } from 'react-router-dom';
import { applyRecommendation, dismissRecommendation, regenerateRecommendations, type RecommendationDto } from '../api/client';
import { requireBranchId } from '../auth/session';

const priorityLabel: Record<string, string> = { HIGH: 'عالية', MEDIUM: 'متوسطة', LOW: 'منخفضة' };
const typeIcon: Record<string, string> = {
  STOP_PURCHASE: '📦',
  INCREASE_ORDER: '📈',
  ADJUST_PRICE: '🏷️',
  PROMOTION_CAMPAIGN: '📣',
  LIQUIDITY_ALERT: '💧',
  EXPIRY_ALERT: '⏰',
};

export function RecommendationsList({ items, onChanged }: { items: RecommendationDto[]; onChanged: () => void }) {
  const [busyId, setBusyId] = useState<number | null>(null);
  const [generating, setGenerating] = useState(false);

  async function handle(action: 'apply' | 'dismiss', id: number) {
    setBusyId(id);
    try {
      if (action === 'apply') await applyRecommendation(id);
      else await dismissRecommendation(id);
      onChanged();
    } finally {
      setBusyId(null);
    }
  }

  async function handleGenerate() {
    setGenerating(true);
    try {
      await regenerateRecommendations(requireBranchId());
      onChanged();
    } finally {
      setGenerating(false);
    }
  }

  return (
    <div className="card">
      <div className="card-title">أهم التوصيات التنفيذية 🎯</div>

      {items.length === 0 && (
        <div style={{ textAlign: 'center', padding: '20px 0' }}>
          <p style={{ color: 'var(--text-secondary)', fontSize: 13, marginBottom: 12 }}>
            لا توجد توصيات مفتوحة حاليًا
          </p>
          <button className="btn-primary" onClick={handleGenerate} disabled={generating}>
            {generating ? 'جارِ التوليد...' : '🔄 توليد التوصيات الآن'}
          </button>
        </div>
      )}

      {items.map((rec) => (
        <div className="recommendation-row" key={rec.id}>
          <span className={`priority-tag priority-${rec.priority}`}>{priorityLabel[rec.priority]}</span>
          <span className="rec-title">{rec.title}</span>
          <span className="rec-value">
            {typeIcon[rec.type] ?? '💡'} {Math.round(rec.expectedValue / 1000)}K
          </span>
          <button
            className="rec-action rec-action-apply"
            title="تطبيق"
            disabled={busyId === rec.id}
            onClick={() => handle('apply', rec.id)}
          >
            ✓
          </button>
          <button
            className="rec-action rec-action-dismiss"
            title="تجاهل"
            disabled={busyId === rec.id}
            onClick={() => handle('dismiss', rec.id)}
          >
            ✕
          </button>
        </div>
      ))}

      <div style={{ textAlign: 'center', marginTop: 10 }}>
        <Link to="/notifications" style={{ color: 'var(--accent-blue)', fontSize: 13, textDecoration: 'none' }}>
          ← عرض جميع التوصيات
        </Link>
      </div>
    </div>
  );
}
