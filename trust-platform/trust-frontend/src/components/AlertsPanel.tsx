import { Link } from 'react-router-dom';
import type { ExecutiveAlertDto } from '../api/client';

const alertIcon: Record<ExecutiveAlertDto['type'], string> = {
  GROUP_ORDER: '🤝',
  LOW_STOCK: '⚠️',
  SLOW_MOVING: '🐢',
};

const alertLink: Record<ExecutiveAlertDto['type'], string> = {
  GROUP_ORDER: '/purchases',
  LOW_STOCK: '/decisions',
  SLOW_MOVING: '/inventory',
};

export function AlertsPanel({ alerts }: { alerts: ExecutiveAlertDto[] }) {
  return (
    <div className="card">
      <div className="card-title">الطلبات والتنبيهات</div>
      {alerts.map((alert) => (
        <Link key={alert.type} to={alertLink[alert.type]} className="recommendation-row" style={{ textDecoration: 'none', color: 'inherit' }}>
          <span>{alertIcon[alert.type]}</span>
          <span className="rec-title">{alert.label}</span>
          <span className="status-chip status-MEDIUM">{alert.count}</span>
        </Link>
      ))}
    </div>
  );
}
