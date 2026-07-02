import { Link } from 'react-router-dom';
import type { ItemDto } from '../api/client';

const statusLabel: Record<string, string> = { FAST: 'سريع', MEDIUM: 'متوسط', SLOW: 'بطيء', STAGNANT: 'راكد' };
const actionLabel: Record<string, string> = {
  STAGNANT: 'أوقف الشراء',
  SLOW: 'حملة تصريف',
  MEDIUM: 'استمر بالطلب',
  FAST: 'زد الطلب',
};

export function AttentionTable({ items }: { items: ItemDto[] }) {
  return (
    <div className="card">
      <div className="card-title">أصناف تحتاج انتباه</div>
      <table className="attention-table">
        <thead>
          <tr>
            <th>الصنف</th>
            <th>المخزون</th>
            <th>الحالة</th>
            <th>الإجراء المقترح</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <tr key={item.id}>
              <td>{item.name}</td>
              <td>{item.quantity.toLocaleString('ar')}</td>
              <td><span className={`status-chip status-${item.movementStatus}`}>{statusLabel[item.movementStatus]}</span></td>
              <td>{actionLabel[item.movementStatus]}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <div style={{ textAlign: 'center', marginTop: 10 }}>
        <Link to="/inventory" style={{ color: 'var(--accent-blue)', fontSize: 13, textDecoration: 'none' }}>
          ← عرض جميع الأصناف
        </Link>
      </div>
    </div>
  );
}
