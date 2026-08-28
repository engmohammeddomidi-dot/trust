import { Link } from 'react-router-dom';
import type { OpportunitySignalDto } from '../api/client';
import { Icon, type IconName } from './Icon';

/**
 * "فرص اليوم" - الطابور الموحَّد على الشاشة الرئيسية.
 *
 * إعادة التأطير مقصودة: التاجر لا يشتري توصية، بل يستثمر فرصة. ولذلك تُعرض الإشارات
 * بأثرها المالي أولًا وبإجراء مقترح واحد، مرتَّبة بالأهم، ومحدودة بخمسة - رؤية المنتج
 * تنصّ على السقف صراحةً لأن قائمة طويلة تُغلَق ولا تُقرأ.
 *
 * التسمية تخصّ سطح القرارات وحده؛ ميزة "التوصيات" القديمة تحتفظ بمسمّاها لأنها نظام
 * منفصل، وخلطهما كان سيزيد الغموض لا يقلّله.
 */

const kindStyle: Record<string, { color: string; icon: IconName; label: string }> = {
  RISK: { color: 'var(--accent-red)', icon: 'risk', label: 'خطر' },
  OPPORTUNITY: { color: 'var(--accent-green)', icon: 'opportunity', label: 'فرصة' },
};

export function TodaysOpportunities({ signals }: { signals: OpportunitySignalDto[] }) {
  return (
    <div className="card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', flexWrap: 'wrap', gap: 8 }}>
        <div className="card-title" style={{ marginBottom: 4, display: 'flex', alignItems: 'center', gap: 8 }}>
          <Icon name="decisions" size={16} /> فرص اليوم
        </div>
        <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>
          أهم ما يستحق وقتك اليوم، مرتَّبًا بالأثر المتوقَّع
        </div>
      </div>

      {signals.length === 0 ? (
        <p style={{ color: 'var(--text-secondary)', fontSize: 13, marginTop: 10 }}>
          لا توجد فرص أو مخاطر تستدعي إجراءً اليوم — وهذه نتيجة جيدة، لا شاشة فارغة.
        </p>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 10 }}>
          {signals.map((s, idx) => {
            const style = kindStyle[s.kind] ?? kindStyle.OPPORTUNITY;
            return (
              <div
                key={`${s.title}-${idx}`}
                style={{
                  display: 'flex', gap: 12, alignItems: 'flex-start', flexWrap: 'wrap',
                  borderInlineStart: `3px solid ${style.color}`,
                  background: 'var(--bg-panel-alt)',
                  borderRadius: 8, padding: '10px 12px',
                }}
              >
                <span style={{ color: style.color, display: 'flex', paddingTop: 2 }} aria-hidden>
                  <Icon name={style.icon} size={15} />
                </span>

                <div style={{ flex: 1, minWidth: 200 }}>
                  <div style={{ fontSize: 13, fontWeight: 600 }}>{s.title}</div>
                  <div style={{ fontSize: 11.5, color: 'var(--text-secondary)', marginTop: 2, lineHeight: 1.6 }}>
                    {s.detail}
                  </div>
                  {s.suggestedAction && (
                    <div style={{ fontSize: 11.5, color: style.color, marginTop: 4 }}>
                      ← {s.suggestedAction}
                    </div>
                  )}
                </div>

                <div style={{ textAlign: 'left', minWidth: 110 }}>
                  <div style={{ fontSize: 10, color: 'var(--text-secondary)' }}>الأثر المتوقَّع</div>
                  <div style={{ fontSize: 14, fontWeight: 700, color: style.color }}>
                    {Math.round(s.expectedImpact).toLocaleString('ar')} شيكل
                  </div>
                </div>
              </div>
            );
          })}

          <Link to="/decisions" style={{ fontSize: 12, color: 'var(--accent-blue)', marginTop: 2 }}>
            عرض جميع قرارات الشراء ←
          </Link>
        </div>
      )}
    </div>
  );
}
