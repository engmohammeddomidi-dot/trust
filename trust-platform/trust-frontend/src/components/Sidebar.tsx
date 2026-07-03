import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { fetchRecommendations } from '../api/client';
import { clearSession, requireBranchId } from '../auth/session';

interface NavItem {
  key: string;
  label: string;
  icon: string;
  path?: string;
}

const items: NavItem[] = [
  { key: 'home', label: 'الرئيسية', icon: '🏠', path: '/' },
  { key: 'sales', label: 'المبيعات', icon: '📈', path: '/sales' },
  { key: 'inventory', label: 'المخزون', icon: '📦', path: '/inventory' },
  { key: 'purchases', label: 'المشتريات', icon: '🛒', path: '/purchases' },
  { key: 'decisions', label: 'قرارات الشراء', icon: '🎯', path: '/decisions' },
  { key: 'profitability', label: 'الربحية', icon: '💰', path: '/profitability' },
  { key: 'liquidity', label: 'السيولة', icon: '💵', path: '/liquidity' },
  { key: 'pricing', label: 'التسعير', icon: '🏷️', path: '/pricing' },
  { key: 'reports', label: 'التقارير', icon: '📄', path: '/reports' },
  { key: 'notifications', label: 'التنبيهات', icon: '🔔', path: '/notifications' },
  { key: 'suppliers', label: 'الموردون', icon: '🚚', path: '/suppliers' },
  { key: 'settings', label: 'الإعدادات', icon: '⚙️', path: '/settings' },
];

export function Sidebar() {
  const location = useLocation();
  const navigate = useNavigate();
  const [openCount, setOpenCount] = useState<number | null>(null);
  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => {
    fetchRecommendations(requireBranchId(), 'OPEN').then((recs) => setOpenCount(recs.length)).catch(() => setOpenCount(null));
  }, [location.pathname]);

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
            <div className="name">TRUST</div>
            <div className="subtitle">المدير التجاري الذكي</div>
          </div>
        </div>
        {items.map((item) => {
          const isActive = item.path === location.pathname;
          const badge = item.key === 'notifications' ? openCount : null;
          const content = (
            <>
              <span>{item.icon}</span>
              <span>{item.label}</span>
              {!!badge && <span className="badge">{badge}</span>}
            </>
          );
          return item.path ? (
            <Link key={item.key} to={item.path} className={`nav-item ${isActive ? 'active' : ''}`}>
              {content}
            </Link>
          ) : (
            <div key={item.key} className="nav-item">
              {content}
            </div>
          );
        })}
        <div style={{ marginTop: 'auto', paddingTop: 20 }}>
          <div className="nav-item" style={{ cursor: 'pointer' }} onClick={handleLogout}>
            <span>🚪</span>
            <span>تسجيل الخروج</span>
          </div>
        </div>
      </aside>
    </>
  );
}
