import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { forgotPassword } from '../api/client';

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState<string | null>(null);
  const [resetToken, setResetToken] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    try {
      const res = await forgotPassword(email);
      setMessage(res.message);
      setResetToken(res.resetToken);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--bg-page)' }}>
      <div className="card" style={{ width: '100%', maxWidth: 380 }}>
        <div style={{ fontWeight: 700, fontSize: 16, marginBottom: 20 }}>استعادة كلمة المرور</div>

        {!message && (
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label>البريد الإلكتروني</label>
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoFocus />
            </div>
            <button type="submit" className="btn-primary" style={{ width: '100%' }} disabled={submitting}>
              {submitting ? 'جارِ الإرسال...' : 'إرسال رابط الاستعادة'}
            </button>
          </form>
        )}

        {message && (
          <>
            <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 14 }}>{message}</p>
            {resetToken && (
              <div style={{
                background: 'var(--accent-amber-bg)', color: 'var(--accent-amber)', padding: '10px 14px',
                borderRadius: 'var(--radius-md)', fontSize: 12, marginBottom: 14,
              }}>
                ⚠️ لا توجد خدمة بريد فعلية بعد — إليك رابط الاستعادة مباشرة كحل مؤقت:
                <br />
                <Link to={`/reset-password?token=${encodeURIComponent(resetToken)}`} style={{ color: 'var(--accent-blue)', wordBreak: 'break-all' }}>
                  فتح صفحة إعادة تعيين كلمة المرور
                </Link>
              </div>
            )}
          </>
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
