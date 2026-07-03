import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { login } from '../api/client';
import { setSession } from '../auth/session';

export function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('owner@trust.demo');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const res = await login(email, password);
      setSession({
        token: res.token,
        refreshToken: res.refreshToken,
        userId: res.user.id,
        name: res.user.name,
        email: res.user.email,
        role: res.user.role as 'OWNER' | 'BRANCH_MANAGER' | 'STAFF' | 'PLATFORM_ADMIN',
        organizationId: res.user.organizationId,
        organizationName: res.user.organizationName,
        branchId: res.user.branchId,
        tosAccepted: res.user.tosAccepted,
      });
      navigate(res.user.role === 'PLATFORM_ADMIN' ? '/admin' : '/', { replace: true });
    } catch (err) {
      const status = (err as { response?: { status?: number; data?: { message?: string } } })?.response?.status;
      if (status === 429) {
        setError('محاولات دخول كثيرة فاشلة - حاول مرة أخرى خلال دقيقة');
      } else {
        setError('البريد الإلكتروني أو كلمة المرور غير صحيحة');
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: 'var(--bg-page)',
    }}>
      <div className="card" style={{ width: '100%', maxWidth: 380 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 24 }}>
          <div className="logo" style={{
            width: 40, height: 40, borderRadius: 10,
            background: 'linear-gradient(135deg, var(--accent-blue), var(--accent-purple))',
            display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700,
          }}>T</div>
          <div>
            <div style={{ fontWeight: 700, fontSize: 16 }}>TRUST</div>
            <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>المدير التجاري الذكي</div>
          </div>
        </div>

        <form onSubmit={handleSubmit}>
          {error && <div className="form-banner-error">{error}</div>}

          <div className="form-group">
            <label>البريد الإلكتروني</label>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoFocus />
          </div>

          <div className="form-group">
            <label>كلمة المرور</label>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
          </div>

          <button type="submit" className="btn-primary" style={{ width: '100%' }} disabled={submitting}>
            {submitting ? 'جارِ الدخول...' : 'تسجيل الدخول'}
          </button>

          <div style={{ textAlign: 'center', marginTop: 14 }}>
            <Link to="/forgot-password" style={{ color: 'var(--accent-blue)', fontSize: 13, textDecoration: 'none' }}>
              نسيت كلمة المرور؟
            </Link>
          </div>
        </form>

        <p style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 16, textAlign: 'center' }}>
          للتجربة: owner@trust.demo / password123 (مؤسسة) — admin@trust.demo / admin123 (أدمن المنصة)
        </p>
      </div>
    </div>
  );
}
