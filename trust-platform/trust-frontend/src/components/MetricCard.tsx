import { Icon, type IconName } from './Icon';

interface MetricCardProps {
  label: string;
  value: string;
  unit?: string;
  deltaLabel: string;
  deltaValue: number;
  icon: IconName;
  iconBg: string;
}

export function MetricCard({ label, value, unit, deltaLabel, deltaValue, icon, iconBg }: MetricCardProps) {
  const isUp = deltaValue >= 0;
  return (
    <div className="card metric-card">
      <div className="icon" style={{ color: iconBg }} aria-hidden>
        <Icon name={icon} size={17} />
      </div>
      <div className="label">{label}</div>
      <div className="value">
        {value} {unit && <span style={{ fontSize: 13, color: 'var(--text-secondary)', fontWeight: 400 }}>{unit}</span>}
      </div>
      {/* الاتجاه يُعلَن بأيقونة ونص معًا، لا باللون وحده */}
      <div className={`delta ${isUp ? 'up' : 'down'}`}>
        <Icon name={isUp ? 'opportunity' : 'declining'} size={13} />
        <span>{Math.abs(deltaValue)}% {deltaLabel}</span>
      </div>
    </div>
  );
}
