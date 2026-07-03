import { useState, type FormEvent } from 'react';
import { Modal } from './Modal';
import { createUser, type UserListDto } from '../api/client';
import { requireBranchId } from '../auth/session';

const roleOptions = [
  { value: 'BRANCH_MANAGER', label: 'مدير فرع' },
  { value: 'STAFF', label: 'موظف' },
];

export function AddUserModal({ onClose, onCreated }: { onClose: () => void; onCreated: (u: UserListDto) => void }) {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('STAFF');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (password.length < 8) {
      setError('كلمة المرور يجب أن تكون 8 أحرف على الأقل');
      return;
    }
    setSubmitting(true);
    try {
      const created = await createUser({ name: name.trim(), email: email.trim(), password, role, branchId: requireBranchId() });
      onCreated(created);
      onClose();
    } catch (err) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(message || 'تعذّر إضافة الموظف. حاول مرة أخرى.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title="إضافة موظف" onClose={onClose}>
      <form onSubmit={handleSubmit}>
        {error && <div className="form-banner-error">{error}</div>}

        <div className="form-group">
          <label>الاسم</label>
          <input value={name} onChange={(e) => setName(e.target.value)} />
        </div>

        <div className="form-group">
          <label>البريد الإلكتروني</label>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>

        <div className="form-group">
          <label>كلمة المرور المبدئية</label>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </div>

        <div className="form-group">
          <label>الدور</label>
          <select
            value={role}
            onChange={(e) => setRole(e.target.value)}
            style={{
              width: '100%', background: 'var(--bg-panel-alt)', border: '1px solid var(--border-subtle)',
              borderRadius: 'var(--radius-md)', padding: '9px 12px', color: 'var(--text-primary)', fontSize: 14,
            }}
          >
            {roleOptions.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
          </select>
        </div>

        <div className="form-actions">
          <button type="button" className="btn-secondary" onClick={onClose}>إلغاء</button>
          <button type="submit" className="btn-primary" disabled={submitting}>
            {submitting ? 'جارِ الإضافة...' : 'إضافة'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
