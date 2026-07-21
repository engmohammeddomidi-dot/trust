interface KpiCardProps {
  icon: string;
  iconBg: string;
  label: string;
  value: string;
  unit?: string;
  caption: string;
  captionColor?: string;
}

export function KpiCard({ icon, iconBg, label, value, unit, caption, captionColor }: KpiCardProps) {
  return (
    <div className="card metric-card kpi-card">
      <div className="icon" style={{ background: iconBg }}>{icon}</div>
      <div className="label">{label}</div>
      <div className="value">
        {value} {unit && <span style={{ fontSize: 13, color: 'var(--text-secondary)', fontWeight: 400 }}>{unit}</span>}
      </div>
      <div className="caption" style={captionColor ? { color: captionColor } : undefined}>{caption}</div>
    </div>
  );
}
