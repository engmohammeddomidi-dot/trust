import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { fetchRecommendations } from '../api/client';
import { clearSession, requireBranchId } from '../auth/session';
import { ThemeToggle } from './ThemeToggle';
import { Icon, type IconName } from './Icon';

interface NavItem {
  key: string;
  label: string;
  icon: IconName;
  path?: string;
}

const items: NavItem[] = [
  { key: 'home', label: 'الرئيسية', icon: 'dashboard', path: '/' },
  { key: 'sales', label: 'المبيعات', icon: 'sales', path: '/sales' },
  { key: 'inventory', label: 'المخزون', icon: 'inventory', path: '/inventory' },
  { key: 'purchases', label: 'المشتريات', icon: 'purchases', path: '/purchases' },
  { key: 'decisions', label: 'قرارات الشراء', icon: 'decisions', path: '/decisions' },
  { key: 'profitability', label: 'الربحية', icon: 'profitability', path: '/profitability' },
  { key: 'liquidity', label: 'السيولة', icon: 'liquidity', path: '/liquidity' },
  { key: 'pricing', label: 'التسعير', icon: 'pricing', path: '/pricing' },
  { key: 'reports', label: 'التقارير', icon: 'reports', path: '/reports' },
  { key: 'notifications', label: 'التنبيهات', icon: 'notifications', path: '/notifications' },
  { key: 'suppliers', label: 'الموردون', icon: 'suppliers', path: '/suppliers' },
  { key: 'settings', label: 'الإعدادات', icon: 'settings', path: '/settings' },
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
      <button className="mobile-menu-btn" onClick={() => setMobileOpen(true)} aria-label="فتح القائمة"><Icon name="menu" size={18} /></button>
      <div className={`sidebar-overlay ${mobileOpen ? 'visible' : ''}`} onClick={() => setMobileOpen(false)} />
      <aside className={`sidebar ${mobileOpen ? 'open' : ''}`}>
        <div className="sidebar-brand">
          <div className="logo">T</div>
          <div className="titles">
            <div className="name">TRUST</div>
            <div className="subtitle">المدير التجاري الذكي</div>
          </div>
          <div style={{ marginInlineStart: 'auto' }}>
            <ThemeToggle />
          </div>
        </div>
        {items.map((item) => {
          const isActive = item.path === location.pathname;
          const badge = item.key === 'notifications' ? openCount : null;
          const content = (
            <>
              <Icon name={item.icon} />
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
            <Icon name="logout" />
            <span>تسجيل الخروج</span>
          </div>
        </div>
      </aside>
    </>
  );
}
