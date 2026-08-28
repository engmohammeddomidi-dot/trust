import { Icon, type IconName } from './Icon';

interface KpiCardProps {
  icon: IconName;
  /** لون دلالي للأيقونة - يُمرَّر كـ var(--accent-*) */
  iconBg: string;
  label: string;
  value: string;
  unit?: string;
  caption: string;
  captionColor?: string;
}

/**
 * بلاطة مؤشر. الأيقونة مُلوَّنة بلونها الدلالي على خلفية خافتة بدل كتلة لونية مصمتة -
 * الكتل المشبعة تتنافس مع الرقم، والرقم هو محتوى البلاطة.
 */
export function KpiCard({ icon, iconBg, label, value, unit, caption, captionColor }: KpiCardProps) {
  return (
    <div className="card metric-card kpi-card">
      <div className="icon" style={{ color: iconBg }} aria-hidden>
        <Icon name={icon} size={17} />
      </div>
      <div className="label">{label}</div>
      <div className="value">
        {value} {unit && <span style={{ fontSize: 13, color: 'var(--text-secondary)', fontWeight: 400 }}>{unit}</span>}
      </div>
      <div className="caption" style={captionColor ? { color: captionColor } : undefined}>{caption}</div>
    </div>
  );
}
