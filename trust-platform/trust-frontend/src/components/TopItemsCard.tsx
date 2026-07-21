import type { TopItemDto } from '../api/client';

function ItemList({ items, valueColor }: { items: TopItemDto[]; valueColor: string }) {
  if (items.length === 0) {
    return <p style={{ color: 'var(--text-secondary)', fontSize: 12 }}>لا توجد بيانات كافية بعد</p>;
  }
  return (
    <>
      {items.map((item) => (
        <div key={item.itemName} style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', fontSize: 13 }}>
          <span>{item.itemName}</span>
          <span style={{ fontWeight: 700, color: valueColor }}>{Math.round(item.value).toLocaleString('ar')} شيكل</span>
        </div>
      ))}
    </>
  );
}

export function TopItemsCard({ topProfitabilityItems, topAccumulatedCostItems }: {
  topProfitabilityItems: TopItemDto[];
  topAccumulatedCostItems: TopItemDto[];
}) {
  return (
    <div className="card">
      <div className="card-title">أعلى الأصناف تأثيرًا هذا الشهر</div>
      <div className="grid-row grid-2" style={{ marginBottom: 0 }}>
        <div>
          <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 6 }}>الأعلى من حيث الربحية المتوقعة</div>
          <ItemList items={topProfitabilityItems} valueColor="var(--accent-green)" />
        </div>
        <div>
          <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 6 }}>الأعلى من حيث التكلفة المتراكمة (راكد/بطيء)</div>
          <ItemList items={topAccumulatedCostItems} valueColor="var(--accent-red)" />
        </div>
      </div>
    </div>
  );
}
