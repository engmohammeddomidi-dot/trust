import { useState } from 'react';
import { acceptTos } from '../api/client';
import { getSession, setSession } from '../auth/session';
import { Modal } from './Modal';

export function TosGateModal({ onAccepted }: { onAccepted: () => void }) {
  const [checked, setChecked] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  async function handleAccept() {
    setSubmitting(true);
    try {
      await acceptTos();
      const session = getSession();
      if (session) setSession({ ...session, tosAccepted: true });
      onAccepted();
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title="شروط الاستخدام" onClose={() => {}}>
      <div style={{ maxHeight: 240, overflowY: 'auto', fontSize: 13, color: 'var(--text-secondary)', marginBottom: 16, lineHeight: 1.7 }}>
        <p>
          باستخدامك منصة TRUST فإنك توافق على استخدام البيانات المُدخَلة (المبيعات، المخزون، السيولة)
          لتوليد مؤشرات صحة الأعمال والتوصيات ضمن حسابك فقط. المنصة لا تشارك بيانات مؤسستك مع مؤسسات
          أخرى إلا في سياق الطلبات الجماعية التي تنضم إليها صراحة (اسم الصنف والكمية فقط، دون تفاصيل مالية
          داخلية). يمكنك تصدير بياناتك أو طلب حذفها في أي وقت عبر التواصل مع الدعم.
        </p>
      </div>

      <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, marginBottom: 16, cursor: 'pointer' }}>
        <input type="checkbox" checked={checked} onChange={(e) => setChecked(e.target.checked)} />
        أوافق على شروط الاستخدام
      </label>

      <div className="form-actions">
        <button className="btn-primary" onClick={handleAccept} disabled={!checked || submitting}>
          {submitting ? 'جارِ الحفظ...' : 'متابعة'}
        </button>
      </div>
    </Modal>
  );
}
