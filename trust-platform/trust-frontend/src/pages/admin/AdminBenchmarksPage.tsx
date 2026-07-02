import { useEffect, useState } from 'react';
import { AdminSidebar } from '../../components/AdminSidebar';
import { fetchAdminBenchmarks, updateAdminBenchmark, type CategoryBenchmarkDto } from '../../api/client';

const categoryLabel: Record<string, string> = {
  SUPERMARKET: 'سوبرماركت',
  PHARMACY: 'صيدلية',
  RESTAURANT: 'مطعم',
  RETAIL_CLOTHING: 'تجارة ملابس',
  GENERAL_TRADE: 'تجارة عامة',
  COMPANY_OTHER: 'أخرى',
};

export function AdminBenchmarksPage() {
  const [benchmarks, setBenchmarks] = useState<CategoryBenchmarkDto[] | null>(null);
  const [savingCategory, setSavingCategory] = useState<string | null>(null);
  const [savedCategory, setSavedCategory] = useState<string | null>(null);

  useEffect(() => {
    fetchAdminBenchmarks().then(setBenchmarks);
  }, []);

  function updateField(category: string, field: keyof CategoryBenchmarkDto, value: number) {
    setBenchmarks((prev) => prev?.map((b) => (b.category === category ? { ...b, [field]: value } : b)) ?? null);
  }

  async function save(bm: CategoryBenchmarkDto) {
    setSavingCategory(bm.category);
    setSavedCategory(null);
    try {
      const { category, ...rest } = bm;
      const updated = await updateAdminBenchmark(category, rest);
      setBenchmarks((prev) => prev?.map((b) => (b.category === category ? updated : b)) ?? null);
      setSavedCategory(category);
      setTimeout(() => setSavedCategory(null), 2000);
    } finally {
      setSavingCategory(null);
    }
  }

  return (
    <div className="app-shell">
      <AdminSidebar />
      <main className="main-area">
        <div className="page-header">
          <div className="page-title">إعدادات المعايير المرجعية لكل تصنيف</div>
        </div>

        <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 16 }}>
          هذه القيم تتحكم بحساب مؤشر صحة الأعمال والتوصيات لكل المؤسسات ضمن نفس التصنيف —
          مثلًا هامش ربح مرجعي أعلى للصيدليات، ونطاق سيولة مختلف للمطاعم.
        </p>

        {benchmarks === null && <p style={{ color: 'var(--text-secondary)' }}>جاري التحميل...</p>}

        {benchmarks !== null && benchmarks.map((bm) => (
          <div className="card" key={bm.category} style={{ marginBottom: 14 }}>
            <div className="card-title">{categoryLabel[bm.category] ?? bm.category}</div>
            <div className="grid-row grid-3" style={{ marginBottom: 0 }}>
              <div className="form-group">
                <label>هامش الربح المرجعي %</label>
                <input
                  type="number" step="0.1" value={bm.targetMarginPercent}
                  onChange={(e) => updateField(bm.category, 'targetMarginPercent', parseFloat(e.target.value) || 0)}
                />
              </div>
              <div className="form-group">
                <label>الحد الأدنى لنسبة السيولة الصحية</label>
                <input
                  type="number" step="0.1" value={bm.liquidityRatioMin}
                  onChange={(e) => updateField(bm.category, 'liquidityRatioMin', parseFloat(e.target.value) || 0)}
                />
              </div>
              <div className="form-group">
                <label>الحد الأعلى لنسبة السيولة الصحية</label>
                <input
                  type="number" step="0.1" value={bm.liquidityRatioMax}
                  onChange={(e) => updateField(bm.category, 'liquidityRatioMax', parseFloat(e.target.value) || 0)}
                />
              </div>
              <div className="form-group">
                <label>أيام بدون بيع لاعتبار الصنف راكدًا</label>
                <input
                  type="number" step="1" value={bm.stagnationDaysThreshold}
                  onChange={(e) => updateField(bm.category, 'stagnationDaysThreshold', parseInt(e.target.value) || 0)}
                />
              </div>
              <div className="form-group">
                <label>أيام بدون بيع لاعتبار الصنف بطيء الحركة</label>
                <input
                  type="number" step="1" value={bm.slowMovingDaysThreshold}
                  onChange={(e) => updateField(bm.category, 'slowMovingDaysThreshold', parseInt(e.target.value) || 0)}
                />
              </div>
              <div className="form-group">
                <label>أيام بدون بيع لاعتبار الصنف متوسط الحركة</label>
                <input
                  type="number" step="1" value={bm.mediumMovingDaysThreshold}
                  onChange={(e) => updateField(bm.category, 'mediumMovingDaysThreshold', parseInt(e.target.value) || 0)}
                />
              </div>
            </div>
            <div className="form-actions" style={{ justifyContent: 'flex-start' }}>
              <button className="btn-primary" onClick={() => save(bm)} disabled={savingCategory === bm.category}>
                {savingCategory === bm.category ? 'جارِ الحفظ...' : 'حفظ'}
              </button>
              {savedCategory === bm.category && <span style={{ color: 'var(--accent-green)', fontSize: 13, alignSelf: 'center' }}>تم الحفظ</span>}
            </div>
          </div>
        ))}
      </main>
    </div>
  );
}
