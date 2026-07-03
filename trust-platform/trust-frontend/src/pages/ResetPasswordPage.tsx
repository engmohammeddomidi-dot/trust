import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { resetPassword } from '../api/client';

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [token, setToken] = useState(searchParams.get('token') ?? '');
  const [newPassword, setNewPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await resetPassword(token, newPassword);
      setSuccess(true);
      setTimeout(() => navigate('/login', { replace: true }), 2000);
    } catch {
      setError('رمز الاستعادة غير صالح أو منتهي الصلاحية');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--bg-page)' }}>
      <div className="card" style={{ width: '100%', maxWidth: 380 }}>
        <div style={{ fontWeight: 700, fontSize: 16, marginBottom: 20 }}>تعيين كلمة مرور جديدة</div>

        {success ? (
          <p style={{ color: 'var(--accent-green)', fontSize: 13 }}>تم تعيين كلمة المرور بنجاح — سيتم تحويلك لصفحة الدخول...</p>
        ) : (
          <form onSubmit={handleSubmit}>
            {error && <div className="form-banner-error">{error}</div>}

            <div className="form-group">
              <label>رمز الاستعادة</label>
              <input value={token} onChange={(e) => setToken(e.target.value)} />
            </div>

            <div className="form-group">
              <label>كلمة المرور الجديدة</label>
              <input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} />
            </div>

            <button type="submit" className="btn-primary" style={{ width: '100%' }} disabled={submitting}>
              {submitting ? 'جارِ الحفظ...' : 'تعيين كلمة المرور'}
            </button>
          </form>
        )}

        <div style={{ textAlign: 'center', marginTop: 14 }}>
          <Link to="/login" style={{ color: 'var(--accent-blue)', fontSize: 13, textDecoration: 'none' }}>
            ← العودة لتسجيل الدخول
          </Link>
        </div>
      </div>
    </div>
  );
}
