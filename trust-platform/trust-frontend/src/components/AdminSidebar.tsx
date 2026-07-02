import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { clearSession, getSession } from '../auth/session';

const items = [
  { key: 'overview', label: 'نظرة عامة', icon: '📊', path: '/admin' },
  { key: 'organizations', label: 'المؤسسات', icon: '🏢', path: '/admin/organizations' },
  { key: 'stagnant', label: 'الأصناف الراكدة', icon: '📦', path: '/admin/stagnant-items' },
  { key: 'group-orders', label: 'الطلبات الجماعية', icon: '🤝', path: '/admin/group-orders' },
  { key: 'benchmarks', label: 'المعايير المرجعية', icon: '⚙️', path: '/admin/benchmarks' },
];

export function AdminSidebar() {
  const location = useLocation();
  const navigate = useNavigate();
  const session = getSession();
  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => {
    setMobileOpen(false);
  }, [location.pathname]);

  function handleLogout() {
    clearSession();
    navigate('/login', { replace: true });
  }

  return (
    <>
      <button className="mobile-menu-btn" onClick={() => setMobileOpen(true)} aria-label="فتح القائمة">☰</button>
      <div className={`sidebar-overlay ${mobileOpen ? 'visible' : ''}`} onClick={() => setMobileOpen(false)} />
      <aside className={`sidebar ${mobileOpen ? 'open' : ''}`}>
        <div className="sidebar-brand">
          <div className="logo">T</div>
          <div className="titles">
            <div className="name">TRUST Admin</div>
            <div className="subtitle">لوحة تحكم المنصة</div>
          </div>
        </div>
        {items.map((item) => (
          <Link key={item.key} to={item.path} className={`nav-item ${location.pathname === item.path ? 'active' : ''}`}>
            <span>{item.icon}</span>
            <span>{item.label}</span>
          </Link>
        ))}
        <div style={{ marginTop: 'auto', paddingTop: 20 }}>
          {session && (
            <div style={{ fontSize: 12, color: 'var(--text-secondary)', padding: '0 12px 10px' }}>
              {session.name}
            </div>
          )}
          <div className="nav-item" style={{ cursor: 'pointer' }} onClick={handleLogout}>
            <span>🚪</span>
            <span>تسجيل الخروج</span>
          </div>
        </div>
      </aside>
    </>
  );
}
