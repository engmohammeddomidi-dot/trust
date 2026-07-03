import { useState } from 'react';
import { Modal } from './Modal';
import { bulkImportItems, type ItemImportRow } from '../api/client';
import { requireBranchId } from '../auth/session';

function parseCsv(text: string): ItemImportRow[] {
  const lines = text.split(/\r?\n/).map((l) => l.trim()).filter((l) => l.length > 0);
  if (lines.length < 2) return [];

  const headers = lines[0].split(',').map((h) => h.trim());
  const rows: ItemImportRow[] = [];
  for (let i = 1; i < lines.length; i++) {
    const cells = lines[i].split(',').map((c) => c.trim());
    const record: Record<string, string> = {};
    headers.forEach((h, idx) => { record[h] = cells[idx] ?? ''; });
    rows.push({
      name: record.name ?? '',
      subCategory: record.subCategory || undefined,
      costPrice: parseFloat(record.costPrice) || 0,
      salePrice: parseFloat(record.salePrice) || 0,
      quantity: parseFloat(record.quantity) || 0,
      lastSaleDate: record.lastSaleDate || undefined,
      expiryDate: record.expiryDate || undefined,
    });
  }
  return rows;
}

export function ImportItemsCsvModal({ onClose, onImported }: { onClose: () => void; onImported: () => void }) {
  const [fileName, setFileName] = useState<string | null>(null);
  const [rows, setRows] = useState<ItemImportRow[]>([]);
  const [result, setResult] = useState<{ createdCount: number; errors: string[] } | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function handleFile(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setFileName(file.name);
    const reader = new FileReader();
    reader.onload = () => {
      const parsed = parseCsv(String(reader.result));
      setRows(parsed);
    };
    reader.readAsText(file);
  }

  async function handleImport() {
    setSubmitting(true);
    try {
      const res = await bulkImportItems(requireBranchId(), rows);
      setResult(res);
      if (res.createdCount > 0) onImported();
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title="استيراد أصناف من CSV" onClose={onClose}>
      {!result && (
        <>
          <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 14 }}>
            الأعمدة المطلوبة (بهذا الترتيب في السطر الأول): name, subCategory, costPrice, salePrice, quantity, lastSaleDate, expiryDate
            <br />
            (subCategory و lastSaleDate و expiryDate اختيارية — التاريخ بصيغة YYYY-MM-DD)
          </p>

          <div className="form-group">
            <label>ملف CSV</label>
            <input type="file" accept=".csv,text/csv" onChange={handleFile} />
          </div>

          {fileName && (
            <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 14 }}>
              تم تحميل {rows.length} صف من {fileName}
            </p>
          )}

          <div className="form-actions">
            <button type="button" className="btn-secondary" onClick={onClose}>إلغاء</button>
            <button type="button" className="btn-primary" onClick={handleImport} disabled={rows.length === 0 || submitting}>
              {submitting ? 'جارِ الاستيراد...' : `استيراد ${rows.length} صنف`}
            </button>
          </div>
        </>
      )}

      {result && (
        <>
          <div className="form-live-margin">تم استيراد {result.createdCount} صنف بنجاح</div>
          {result.errors.length > 0 && (
            <div className="form-banner-error">
              {result.errors.length} خطأ:
              <ul style={{ margin: '6px 0 0', paddingInlineStart: 18 }}>
                {result.errors.map((err, idx) => <li key={idx}>{err}</li>)}
              </ul>
            </div>
          )}
          <div className="form-actions">
            <button className="btn-primary" onClick={onClose}>إغلاق</button>
          </div>
        </>
      )}
    </Modal>
  );
}
