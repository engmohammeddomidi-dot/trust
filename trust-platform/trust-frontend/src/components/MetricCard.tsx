interface MetricCardProps {
  label: string;
  value: string;
  unit?: string;
  deltaLabel: string;
  deltaValue: number;
  icon: string;
  iconBg: string;
}

export function MetricCard({ label, value, unit, deltaLabel, deltaValue, icon, iconBg }: MetricCardProps) {
  const isUp = deltaValue >= 0;
  return (
    <div className="card metric-card">
      <div className="icon" style={{ background: iconBg }}>{icon}</div>
      <div className="label">{label}</div>
      <div className="value">
        {value} {unit && <span style={{ fontSize: 13, color: 'var(--text-secondary)', fontWeight: 400 }}>{unit}</span>}
      </div>
      <div className={`delta ${isUp ? 'up' : 'down'}`}>
        <span>{isUp ? '▲' : '▼'}</span>
        <span>{Math.abs(deltaValue)}% {deltaLabel}</span>
      </div>
    </div>
  );
}
