interface ImpactRow {
  label: string;
  value: number;
  color: string;
}

export function ImpactLedgerCard({ purchaseCostSavings, inventoryRiskImpact, operatingProfitImpact, total }: {
  purchaseCostSavings: number;
  inventoryRiskImpact: number;
  operatingProfitImpact: number;
  total: number;
}) {
  const rows: ImpactRow[] = [
    { label: 'توفير في تكلفة المشتريات (شراء جماعي)', value: purchaseCostSavings, color: 'var(--accent-green)' },
    { label: 'أثر معالجة مخاطر المخزون', value: inventoryRiskImpact, color: 'var(--accent-blue)' },
    { label: 'زيادة ربحية التشغيل', value: operatingProfitImpact, color: 'var(--accent-purple)' },
  ];
  const maxValue = Math.max(1, ...rows.map((r) => r.value));

  return (
    <div className="card">
      <div className="card-title">الأثر الفعلي لهذا الشهر</div>
      {rows.map((row) => (
        <div key={row.label} style={{ marginBottom: 14 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, marginBottom: 6 }}>
            <span style={{ color: 'var(--text-secondary)' }}>{row.label}</span>
            <span style={{ fontWeight: 700 }}>{Math.round(row.value).toLocaleString('ar')} شيكل</span>
          </div>
          <div style={{ height: 8, borderRadius: 4, background: 'var(--bg-panel-alt)', overflow: 'hidden' }}>
            <div style={{ height: '100%', width: `${Math.min(100, (row.value / maxValue) * 100)}%`, background: row.color, borderRadius: 4 }} />
          </div>
        </div>
      ))}
      <div style={{ display: 'flex', justifyContent: 'space-between', paddingTop: 10, borderTop: '1px solid var(--border-subtle)' }}>
        <span style={{ fontWeight: 700 }}>إجمالي الأثر المالي</span>
        <span style={{ fontWeight: 700, color: 'var(--accent-green)' }}>{Math.round(total).toLocaleString('ar')} شيكل</span>
      </div>
    </div>
  );
}
