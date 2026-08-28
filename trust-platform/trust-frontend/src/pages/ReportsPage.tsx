import { useState } from 'react';
import { Sidebar } from '../components/Sidebar';
import { fetchDailyEntries, fetchItems, type ItemDto } from '../api/client';
import { requireBranchId } from '../auth/session';

function downloadCsv(filename: string, headers: string[], rows: (string | number)[][]) {
  const csvLines = [headers.join(','), ...rows.map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(','))];
  const csvContent = '﻿' + csvLines.join('\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

function fmtDate(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

const statusLabel: Record<string, string> = { FAST: 'سريع', MEDIUM: 'متوسط', SLOW: 'بطيء', STAGNANT: 'راكد' };

export function ReportsPage() {
  const [exporting, setExporting] = useState<string | null>(null);

  async function exportInventoryReport(onlyStagnant: boolean) {
    setExporting(onlyStagnant ? 'stagnant' : 'inventory');
    try {
      const items = await fetchItems(requireBranchId());
      const filtered = onlyStagnant ? items.filter((i: ItemDto) => i.movementStatus === 'STAGNANT') : items;
      downloadCsv(
        onlyStagnant ? 'تقرير-المخزون-الراكد.csv' : 'تقرير-المخزون-الكامل.csv',
        ['الاسم', 'الفئة الفرعية', 'سعر التكلفة', 'سعر البيع', 'هامش الربح %', 'الكمية', 'القيمة', 'الحالة', 'تاريخ الانتهاء'],
        filtered.map((i) => [
          i.name, i.subCategory ?? '', i.costPrice, i.salePrice, i.marginPercent.toFixed(1),
          i.quantity, i.inventoryValue, statusLabel[i.movementStatus], i.expiryDate ?? '',
        ])
      );
    } finally {
      setExporting(null);
    }
  }

  async function exportSalesReport() {
    setExporting('sales');
    try {
      const to = new Date();
      const from = new Date();
      from.setDate(from.getDate() - 30);
      const entries = await fetchDailyEntries(requireBranchId(), fmtDate(from), fmtDate(to));
      downloadCsv(
        'تقرير-المبيعات-30-يوم.csv',
        ['التاريخ', 'المبيعات', 'التكلفة', 'الربح', 'الهامش %', 'السيولة المتاحة', 'الذمم المدينة', 'الالتزامات الحالة'],
        entries.map((e) => [e.entryDate, e.totalSales, e.totalCogs, e.totalProfit, e.marginPercent.toFixed(1), e.availableLiquidity, e.receivables, e.payables])
      );
    } finally {
      setExporting(null);
    }
  }

  const reports = [
    { key: 'inventory', title: 'تقرير المخزون الكامل', desc: 'جميع الأصناف مع الأسعار والهامش والحالة', action: () => exportInventoryReport(false) },
    { key: 'stagnant', title: 'تقرير المخزون الراكد', desc: 'الأصناف الراكدة فقط — لتحديد أولويات التصريف', action: () => exportInventoryReport(true) },
    { key: 'sales', title: 'تقرير المبيعات (آخر 30 يوم)', desc: 'الإدخالات اليومية للمبيعات والربح والسيولة', action: exportSalesReport },
  ];

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-area">
        <div className="page-header">
          <div className="page-title">التقارير</div>
        </div>

        <div className="grid-row grid-3">
          {reports.map((r) => (
            <div className="card" key={r.key}>
              <div className="card-title">{r.title}</div>
              <p style={{ color: 'var(--text-secondary)', fontSize: 13, marginBottom: 16 }}>{r.desc}</p>
              <button className="btn-primary" onClick={r.action} disabled={exporting === r.key}>
                {exporting === r.key ? 'جارِ التصدير...' : 'تصدير CSV'}
              </button>
            </div>
          ))}
        </div>
      </main>
    </div>
  );
}
