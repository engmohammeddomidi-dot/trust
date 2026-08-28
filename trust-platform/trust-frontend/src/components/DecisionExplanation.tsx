import type { DecisionAlternative, DecisionDto } from '../api/client';

/**
 * الجزء الذي يحوّل بطاقة القرار من إعلان إلى استشارة.
 *
 * ثلاثة عناصر يفرضها دستور رؤية المنتج ولم تكن معروضة: أثر التجاهل (الوجه الآخر
 * للتوصية)، القيود التي راعاها المحرك، وأسباب درجة الثقة بدل نسبة مجرّدة. ثم البدائل،
 * لأن "لا توجد توصية دون بدائل" - الخيار الواحد يُقرأ كأمر، والثلاثة تُبقي القرار
 * بيد صاحب المحل.
 */

function bullets(value: string | null): string[] {
  if (!value) return [];
  return value.split('•').map((s) => s.trim()).filter(Boolean);
}

function AlternativeRow({ alt, onChoose, disabled }: {
  alt: DecisionAlternative;
  onChoose?: (quantity: number) => void;
  disabled?: boolean;
}) {
  const border = alt.recommended ? 'var(--accent-green)' : 'var(--border-subtle)';

  return (
    <div
      style={{
        border: `1px solid ${border}`,
        borderRadius: 10,
        padding: '10px 12px',
        display: 'flex',
        gap: 12,
        alignItems: 'flex-start',
        flexWrap: 'wrap',
      }}
    >
      <div style={{ flex: 1, minWidth: 200 }}>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <strong style={{ fontSize: 13 }}>{alt.label}</strong>
          {alt.recommended && (
            <span style={{ fontSize: 10, color: 'var(--accent-green)', border: '1px solid var(--accent-green)',
              borderRadius: 999, padding: '1px 8px' }}>
              الأنسب
            </span>
          )}
          {alt.liquidityLimited && (
            <span style={{ fontSize: 10, color: 'var(--accent-amber)' }}>مقيَّد بالسيولة</span>
          )}
        </div>
        <div style={{ fontSize: 11, color: 'var(--text-secondary)', marginTop: 3, lineHeight: 1.6 }}>
          {alt.tradeOff}
        </div>
      </div>

      <div style={{ textAlign: 'left', fontSize: 12, minWidth: 130 }}>
        <div><strong>{alt.quantity.toLocaleString('ar')}</strong> وحدة</div>
        <div style={{ color: 'var(--text-secondary)' }}>
          {Math.round(alt.orderValue).toLocaleString('ar')} شيكل
        </div>
        <div style={{ color: 'var(--text-secondary)' }}>
          تغطية ~{alt.coverageDays.toFixed(0)} يوم
        </div>
      </div>

      {onChoose && (
        <button
          className={alt.recommended ? 'btn-primary' : 'btn-secondary'}
          style={{ padding: '6px 14px', fontSize: 12, alignSelf: 'center' }}
          disabled={disabled}
          onClick={() => onChoose(alt.quantity)}
        >
          اعتماد هذا الخيار
        </button>
      )}
    </div>
  );
}

export function DecisionExplanation({ decision, onChooseAlternative, disabled }: {
  decision: DecisionDto;
  /** يمر عبر مسار الاعتماد نفسه، فينشئ أمر شراء حقيقيًا كأي اعتماد آخر */
  onChooseAlternative?: (quantity: number) => void;
  disabled?: boolean;
}) {
  const constraints = bullets(decision.constraintsSummary);
  const reasons = bullets(decision.confidenceReasons);
  const hasAlternatives = decision.alternatives && decision.alternatives.length > 1;

  return (
    <div style={{ marginTop: 10, display: 'flex', flexDirection: 'column', gap: 10 }}>
      {decision.ifIgnoredSummary && (
        <div
          style={{
            fontSize: 12.5, lineHeight: 1.7, color: 'var(--accent-amber)',
            background: 'var(--accent-amber-bg, transparent)',
            border: '1px solid var(--accent-amber)', borderRadius: 8, padding: '8px 12px',
          }}
        >
          {decision.ifIgnoredSummary}
        </div>
      )}

      {(constraints.length > 0 || reasons.length > 0) && (
        <div style={{ display: 'flex', gap: 18, flexWrap: 'wrap' }}>
          {constraints.length > 0 && (
            <div style={{ flex: 1, minWidth: 220 }}>
              <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--text-secondary)', marginBottom: 4 }}>
                القيود التي رُوعيت
              </div>
              <ul style={{ margin: 0, paddingInlineStart: 18, fontSize: 11.5, color: 'var(--text-secondary)', lineHeight: 1.8 }}>
                {constraints.map((c) => <li key={c}>{c}</li>)}
              </ul>
            </div>
          )}

          {reasons.length > 0 && (
            <div style={{ flex: 1, minWidth: 220 }}>
              <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--text-secondary)', marginBottom: 4 }}>
                لماذا هذه درجة الثقة
              </div>
              <ul style={{ margin: 0, paddingInlineStart: 18, fontSize: 11.5, color: 'var(--text-secondary)', lineHeight: 1.8 }}>
                {reasons.map((r) => <li key={r}>{r}</li>)}
              </ul>
            </div>
          )}
        </div>
      )}

      {hasAlternatives && (
        <div>
          <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--text-secondary)', marginBottom: 6 }}>
            البدائل المتاحة
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {decision.alternatives.map((a) => (
              <AlternativeRow key={a.key} alt={a} onChoose={onChooseAlternative} disabled={disabled} />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
