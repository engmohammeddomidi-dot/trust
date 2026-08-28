import axios from 'axios';
import { clearSession, getSession, updateTokens } from '../auth/session';

// في التطوير المحلي (npm run dev) نتصل بـ backend منفصل على 8080. في الإنتاج، إن لم يُحدَّد
// VITE_API_BASE_URL صراحةً، نفترض أن الواجهة تُقدَّم من نفس الأصل الذي يخدم الـ API (نشر مدمج).
const BASE_URL = import.meta.env.VITE_API_BASE_URL || (import.meta.env.DEV ? 'http://localhost:8080/api' : '/api');

export const apiClient = axios.create({ baseURL: BASE_URL });

apiClient.interceptors.request.use((config) => {
  const session = getSession();
  if (session?.token) {
    config.headers.Authorization = `Bearer ${session.token}`;
  }
  return config;
});

let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const session = getSession();
  if (!session?.refreshToken) return null;
  try {
    const { data } = await axios.post(`${BASE_URL}/auth/refresh`, { refreshToken: session.refreshToken });
    updateTokens(data.token, data.refreshToken);
    return data.token as string;
  } catch {
    return null;
  }
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const isAuthEndpoint = originalRequest?.url?.includes('/auth/login') || originalRequest?.url?.includes('/auth/refresh');

    if (error.response?.status === 401 && !isAuthEndpoint && !originalRequest._retried) {
      originalRequest._retried = true;
      refreshPromise = refreshPromise ?? refreshAccessToken().finally(() => { refreshPromise = null; });
      const newToken = await refreshPromise;
      if (newToken) {
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return apiClient(originalRequest);
      }
    }

    if (error.response?.status === 401 && !window.location.pathname.startsWith('/login')) {
      clearSession();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

/** مؤشر واحد من مؤشرات صحة الأعمال الثلاثة عشر */
export interface BhiIndicatorScore {
  code: string;
  labelAr: string;
  available: boolean;
  rawValue: number | null;
  score: number | null;
  band: 'EXCELLENT' | 'GOOD' | 'ACCEPTABLE' | 'WEAK' | 'UNAVAILABLE';
  bandLabelAr: string;
  /** شرح مقروء لموقع القيمة بين الحدود، أو سبب عدم توفّر البيانات */
  explanation: string;
}

export interface BhiAxisScore {
  axis: string;
  labelAr: string;
  /** null يعني أن المحور بلا أي مؤشر متاح - يُعرض ولا يُخفى، لكنه خارج الحساب */
  score: number | null;
  weight: number;
  indicators: BhiIndicatorScore[];
}

/**
 * نتيجة مؤشر صحة الأعمال (BHI) وفق النموذج المرجعي المعتمد.
 * totalScore قد يكون null حين لا تكفي البيانات - نعرض حينها "غير كافٍ" لا صفرًا.
 */
export interface BhiResultDto {
  totalScore: number | null;
  label: string;
  availableIndicatorCount: number;
  totalIndicatorCount: number;
  axes: BhiAxisScore[];
}

export async function fetchBhi(branchId: number, from?: string, to?: string) {
  const { data } = await apiClient.get<BhiResultDto>('/bhi', { params: { branchId, from, to } });
  return data;
}


// ---------------- المصاريف التشغيلية الشهرية ----------------

export interface MonthlyExpenseDto {
  id: number | null;
  month: string | null;
  category: string;
  categoryLabelAr: string;
  unitAmount: number;
  quantity: number;
  total: number;
  note: string | null;
}

export interface ExpenseCategoryDto {
  code: string;
  labelAr: string;
}

export async function fetchExpenses(branchId: number, month?: string): Promise<MonthlyExpenseDto[]> {
  const { data } = await apiClient.get<MonthlyExpenseDto[]>('/expenses', { params: { branchId, month } });
  return data;
}

export async function fetchExpenseCategories(): Promise<ExpenseCategoryDto[]> {
  const { data } = await apiClient.get<ExpenseCategoryDto[]>('/expenses/categories');
  return data;
}

export async function saveExpense(req: {
  branchId: number; month: string; category: string;
  unitAmount: number; quantity: number; note?: string;
}): Promise<MonthlyExpenseDto> {
  const { data } = await apiClient.put<MonthlyExpenseDto>('/expenses', req);
  return data;
}

export async function deleteExpense(id: number, branchId: number): Promise<void> {
  await apiClient.delete(`/expenses/${id}`, { params: { branchId } });
}

// ---------------- التوالف والجرد الفعلي ----------------

export interface WasteRecordDto {
  id: number;
  itemId: number;
  itemName: string;
  wasteDate: string;
  quantity: number;
  unitCost: number;
  totalCost: number;
  reason: string;
  note: string | null;
}

export interface StockCountDto {
  id: number;
  itemId: number;
  itemName: string;
  countDate: string;
  expectedQuantity: number;
  countedQuantity: number;
  discrepancy: number;
  note: string | null;
}

export async function fetchWaste(branchId: number): Promise<WasteRecordDto[]> {
  const { data } = await apiClient.get<WasteRecordDto[]>('/waste', { params: { branchId } });
  return data;
}

export async function recordWaste(req: {
  branchId: number; itemId: number; quantity: number; reason: string; note?: string;
}): Promise<WasteRecordDto> {
  const { data } = await apiClient.post<WasteRecordDto>('/waste', req);
  return data;
}

export async function fetchStockCounts(branchId: number): Promise<StockCountDto[]> {
  const { data } = await apiClient.get<StockCountDto[]>('/stock-counts', { params: { branchId } });
  return data;
}

export async function recordStockCount(req: {
  branchId: number; itemId: number; countedQuantity: number; note?: string;
}): Promise<StockCountDto> {
  const { data } = await apiClient.post<StockCountDto>('/stock-counts', req);
  return data;
}

// ---------------- معايرة نموذج BHI (مشرف المنصة) ----------------

export interface BhiIndicatorConfig {
  code: string;
  labelAr: string;
  axis: string;
  direction: 'HIGHER_BETTER' | 'LOWER_BETTER';
  unit: string;
  weak: number;
  medium: number;
  excellent: number;
  overridden: boolean;
}

export interface BhiAxisConfig {
  axis: string;
  labelAr: string;
  weight: number;
  overridden: boolean;
}

export interface BhiConfigDto {
  category: string;
  indicators: BhiIndicatorConfig[];
  axes: BhiAxisConfig[];
  axisWeightSum: number;
}

export async function fetchBhiConfig(category: string): Promise<BhiConfigDto> {
  const { data } = await apiClient.get<BhiConfigDto>('/admin/bhi-config', { params: { category } });
  return data;
}

export async function saveBhiThreshold(req: {
  category: string; code: string; weak: number; medium: number; excellent: number;
}): Promise<BhiConfigDto> {
  const { data } = await apiClient.put<BhiConfigDto>('/admin/bhi-config/thresholds', req);
  return data;
}

export async function saveBhiWeight(req: {
  category: string; axis: string; weight: number;
}): Promise<BhiConfigDto> {
  const { data } = await apiClient.put<BhiConfigDto>('/admin/bhi-config/weights', req);
  return data;
}

export async function resetBhiThreshold(code: string, category: string): Promise<BhiConfigDto> {
  const { data } = await apiClient.delete<BhiConfigDto>(`/admin/bhi-config/thresholds/${code}`, { params: { category } });
  return data;
}

export async function resetBhiWeight(axis: string, category: string): Promise<BhiConfigDto> {
  const { data } = await apiClient.delete<BhiConfigDto>(`/admin/bhi-config/weights/${axis}`, { params: { category } });
  return data;
}

export interface RecommendationDto {
  id: number;
  type: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  title: string;
  expectedValue: number;
  status: string;
}

export interface ItemDto {
  id: number;
  name: string;
  subCategory: string | null;
  costPrice: number;
  salePrice: number;
  marginPercent: number;
  quantity: number;
  inventoryValue: number;
  lastSaleDate: string | null;
  expiryDate: string | null;
  movementStatus: 'FAST' | 'MEDIUM' | 'SLOW' | 'STAGNANT';
  supplierId: number | null;
  supplierName: string | null;
  safetyStockDays: number;
}

export async function linkItemSupplier(itemId: number, supplierId: number, safetyStockDays?: number): Promise<ItemDto> {
  const { data } = await apiClient.patch<ItemDto>(`/items/${itemId}/supplier`, { supplierId, safetyStockDays });
  return data;
}

export interface DailyPerformanceSummaryDto {
  groupBuySavingsRatePercent: number;
  groupBuySavingsAmountThisMonth: number;
  inventoryTurnoverRatePercent: number;
  purchaseVolumeNeeded: number;
  clearanceVolumeNeeded: number;
}

export interface PerformanceImpactSummaryDto {
  performanceScore: number | null;
  risksResolvedCount: number;
  opportunitiesResolvedCount: number;
  recommendationsCompletionRatePercent: number;
}

export interface TopItemDto {
  itemName: string;
  value: number;
}

export interface ExecutiveAlertDto {
  type: 'GROUP_ORDER' | 'LOW_STOCK' | 'SLOW_MOVING';
  label: string;
  count: number;
}

export interface OpportunitySignalDto {
  kind: 'RISK' | 'OPPORTUNITY';
  title: string;
  detail: string;
  expectedImpact: number;
  suggestedAction: string | null;
  itemId: number | null;
}

export interface ExecutiveActionCenterDto {
  topProfitabilityItems: TopItemDto[];
  topAccumulatedCostItems: TopItemDto[];
  alerts: ExecutiveAlertDto[];
  /** طابور فرص اليوم الموحَّد، مرتَّب بالأثر ومحدود بخمسة */
  todaysOpportunities: OpportunitySignalDto[];
}

export interface MonthlyImpactLedgerDto {
  purchaseCostSavings: number;
  inventoryRiskImpact: number;
  operatingProfitImpact: number;
  totalFinancialImpact: number;
  performanceTrend: { date: string; score: number }[];
}

export interface DashboardResponse {
  salesToday: number;
  salesChangePercent: number;
  totalProfit: number;
  profitChangePercent: number;
  marginPercent: number;
  marginChangePercent: number;
  availableLiquidity: number;
  liquidityChangePercent: number;
  healthScore: BhiResultDto;
  salesTrend: { date: string; sales: number }[];
  topRecommendations: RecommendationDto[];
  inventoryBreakdown: Record<string, number>;
  liquidityBreakdown: Record<string, number>;
  itemsNeedingAttention: ItemDto[];
  dailyPerformanceSummary: DailyPerformanceSummaryDto;
  performanceImpactSummary: PerformanceImpactSummaryDto;
  monthlyImpactLedger: MonthlyImpactLedgerDto;
  executiveActionCenter: ExecutiveActionCenterDto;
}

export async function fetchDashboard(params: {
  organizationId: number;
  branchId?: number;
  from?: string;
  to?: string;
}): Promise<DashboardResponse> {
  const { data } = await apiClient.get<DashboardResponse>('/dashboard', { params });
  return data;
}

export interface ItemCreateRequest {
  branchId: number;
  name: string;
  subCategory?: string;
  costPrice: number;
  salePrice: number;
  quantity: number;
  lastSaleDate?: string;
  expiryDate?: string;
}

export async function fetchItems(branchId: number): Promise<ItemDto[]> {
  const { data } = await apiClient.get<ItemDto[]>('/items', { params: { branchId } });
  return data;
}

export async function createItem(req: ItemCreateRequest): Promise<ItemDto> {
  const { data } = await apiClient.post<ItemDto>('/items', req);
  return data;
}

export interface DailyEntryRequest {
  branchId: number;
  entryDate: string;
  totalSales: number;
  totalCogs: number;
  totalProfit?: number | null;
  availableLiquidity: number;
  receivables: number;
  payables: number;
}

export interface DailyEntryDto {
  id: number;
  branchId: number;
  entryDate: string;
  totalSales: number;
  totalCogs: number;
  totalProfit: number;
  marginPercent: number;
  availableLiquidity: number;
  receivables: number;
  payables: number;
}

export async function submitDailyEntry(req: DailyEntryRequest): Promise<DailyEntryDto> {
  const { data } = await apiClient.post<DailyEntryDto>('/entries/daily', req);
  return data;
}

export async function fetchDailyEntries(branchId: number, from: string, to: string): Promise<DailyEntryDto[]> {
  const { data } = await apiClient.get<DailyEntryDto[]>('/entries/daily', { params: { branchId, from, to } });
  return data;
}

export interface BenchmarkDto {
  targetMarginPercent: number;
  marginRangeLow: number;
  marginRangeHigh: number;
  liquidityRatioMin: number;
  liquidityRatioMax: number;
}

export async function fetchBenchmark(branchId: number): Promise<BenchmarkDto> {
  const { data } = await apiClient.get<BenchmarkDto>('/benchmark', { params: { branchId } });
  return data;
}

export async function fetchRecommendations(branchId: number, status?: 'OPEN' | 'APPLIED' | 'DISMISSED'): Promise<RecommendationDto[]> {
  const { data } = await apiClient.get<RecommendationDto[]>('/recommendations', { params: { branchId, status } });
  return data;
}

export async function applyRecommendation(id: number): Promise<RecommendationDto> {
  const { data } = await apiClient.patch<RecommendationDto>(`/recommendations/${id}/apply`);
  return data;
}

export async function dismissRecommendation(id: number): Promise<RecommendationDto> {
  const { data } = await apiClient.patch<RecommendationDto>(`/recommendations/${id}/dismiss`);
  return data;
}

export async function regenerateRecommendations(branchId: number): Promise<RecommendationDto[]> {
  const { data } = await apiClient.post<RecommendationDto[]>('/recommendations/regenerate', null, { params: { branchId } });
  return data;
}

export interface OrganizationDto {
  id: number;
  name: string;
  category: string;
  /** null يعني لم تُسجَّل بعد - مؤشر نسبة الدين يبقى "غير متاح" */
  equity: number | null;
}

export interface BranchDto {
  id: number;
  organizationId: number;
  name: string;
  city: string | null;
  active: boolean;
}

export async function fetchOrganization(id: number): Promise<OrganizationDto> {
  const { data } = await apiClient.get<OrganizationDto>(`/organizations/${id}`);
  return data;
}

export async function updateOrganization(id: number, name: string, equity?: number | null): Promise<OrganizationDto> {
  const { data } = await apiClient.put<OrganizationDto>(`/organizations/${id}`, { name, equity: equity ?? null });
  return data;
}

export async function fetchBranches(organizationId: number): Promise<BranchDto[]> {
  const { data } = await apiClient.get<BranchDto[]>('/branches', { params: { organizationId } });
  return data;
}

export async function updateBranch(id: number, req: { name: string; city?: string; active: boolean }): Promise<BranchDto> {
  const { data } = await apiClient.put<BranchDto>(`/branches/${id}`, req);
  return data;
}

export interface PurchaseDto {
  id: number;
  itemId: number | null;
  itemName: string | null;
  decisionId: number | null;
  supplierId: number | null;
  supplierName: string;
  quantity: number;
  costPrice: number;
  totalCost: number;
  purchaseDate: string;
  status: 'SENT' | 'RECEIVED';
  receivedQuantity: number | null;
  receivedDate: string | null;
  priceMatched: boolean | null;
  hasDamage: boolean;
  hasDiscrepancy: boolean;
}

export interface PurchaseCreateRequest {
  branchId: number;
  itemId?: number;
  supplierName: string;
  quantity: number;
  costPrice: number;
  purchaseDate: string;
}

export async function fetchPurchases(branchId: number): Promise<PurchaseDto[]> {
  const { data } = await apiClient.get<PurchaseDto[]>('/purchases', { params: { branchId } });
  return data;
}

export async function createPurchase(req: PurchaseCreateRequest): Promise<PurchaseDto> {
  const { data } = await apiClient.post<PurchaseDto>('/purchases', req);
  return data;
}

export async function receivePurchase(id: number, req: { receivedQuantity: number; priceMatched: boolean; hasDamage: boolean }): Promise<PurchaseDto> {
  const { data } = await apiClient.patch<PurchaseDto>(`/purchases/${id}/receive`, req);
  return data;
}

export interface SupplierDto {
  id: number;
  name: string;
  contactInfo: string | null;
  email: string | null;
  leadTimeDays: number;
  creditTermsDays: number;
  rating: number;
}

export async function fetchSuppliers(organizationId: number): Promise<SupplierDto[]> {
  const { data } = await apiClient.get<SupplierDto[]>('/suppliers', { params: { organizationId } });
  return data;
}

export async function createSupplier(req: { organizationId: number; name: string; contactInfo?: string; email?: string; leadTimeDays: number; creditTermsDays: number; rating: number }): Promise<SupplierDto> {
  const { data } = await apiClient.post<SupplierDto>('/suppliers', req);
  return data;
}

export async function updateSupplier(id: number, req: { name: string; contactInfo?: string; email?: string; leadTimeDays: number; creditTermsDays: number; rating: number }): Promise<SupplierDto> {
  const { data } = await apiClient.put<SupplierDto>(`/suppliers/${id}`, req);
  return data;
}

export interface DecisionDto {
  id: number;
  itemId: number;
  itemName: string;
  supplierId: number | null;
  supplierName: string | null;
  type: 'PURCHASE_ORDER';
  category: 'RISK' | 'OPPORTUNITY';
  status: 'OPEN' | 'APPROVED' | 'MODIFIED' | 'DEFERRED' | 'DISMISSED';
  suggestedQuantity: number;
  approvedQuantity: number | null;
  reasonSummary: string;
  confidenceScore: number;
  financialImpact: number;
  createdAt: string;
  resolvedAt: string | null;
  actualOutcome: string | null;
  ifIgnoredSummary: string | null;
  constraintsSummary: string | null;
  confidenceReasons: string | null;
  alternatives: DecisionAlternative[];
}

export interface DecisionAlternative {
  key: string;
  label: string;
  quantity: number;
  orderValue: number;
  coverageDays: number;
  recommended: boolean;
  liquidityLimited: boolean;
  tradeOff: string;
}

export async function fetchDecisions(branchId: number, status?: DecisionDto['status']): Promise<DecisionDto[]> {
  const { data } = await apiClient.get<DecisionDto[]>('/decisions', { params: { branchId, status } });
  return data;
}

export async function regenerateDecisions(branchId: number): Promise<DecisionDto[]> {
  const { data } = await apiClient.post<DecisionDto[]>('/decisions/regenerate', null, { params: { branchId } });
  return data;
}

export async function approveDecision(id: number): Promise<DecisionDto> {
  const { data } = await apiClient.patch<DecisionDto>(`/decisions/${id}/approve`);
  return data;
}

export async function modifyDecision(id: number, quantity: number, supplierId?: number): Promise<DecisionDto> {
  const { data } = await apiClient.patch<DecisionDto>(`/decisions/${id}/modify`, { quantity, supplierId });
  return data;
}

export async function deferDecision(id: number): Promise<DecisionDto> {
  const { data } = await apiClient.patch<DecisionDto>(`/decisions/${id}/defer`);
  return data;
}

export async function dismissDecision(id: number): Promise<DecisionDto> {
  const { data } = await apiClient.patch<DecisionDto>(`/decisions/${id}/dismiss`);
  return data;
}

export interface DecisionQualityScoreDto {
  ordersIssued: number;
  ordersReceived: number;
  ordersWithDiscrepancy: number;
  qualityScorePercent: number | null;
}

export async function fetchDecisionQualityScore(branchId: number): Promise<DecisionQualityScoreDto> {
  const { data } = await apiClient.get<DecisionQualityScoreDto>('/decisions/quality-score', { params: { branchId } });
  return data;
}

export interface PolicyDto {
  maxPurchaseLiquidityRatio: number;
  minSupplierRating: number;
}

export async function fetchPolicy(organizationId: number): Promise<PolicyDto> {
  const { data } = await apiClient.get<PolicyDto>('/policies', { params: { organizationId } });
  return data;
}

export async function updatePolicy(organizationId: number, req: PolicyDto): Promise<PolicyDto> {
  const { data } = await apiClient.put<PolicyDto>('/policies', req, { params: { organizationId } });
  return data;
}

export type GoalType =
  | 'INCREASE_PROFITABILITY' | 'IMPROVE_LIQUIDITY' | 'PREVENT_STOCKOUTS' | 'REDUCE_STAGNANT_INVENTORY'
  | 'INCREASE_SALES' | 'IMPROVE_SUPPLIER_PERFORMANCE' | 'INCREASE_INVENTORY_TURNOVER';

export interface GoalDto {
  type: GoalType;
  labelAr: string;
  pillar: 'PROFITABILITY' | 'WORKING_CAPITAL' | 'OPERATIONAL_EFFICIENCY';
  pillarLabelAr: string;
  priority: number;
  /** false يعني أن الهدف مسجَّل ولا يؤثّر في أي محرك بعد */
  influencesEngine: boolean;
}

export async function fetchGoals(organizationId: number): Promise<GoalDto[]> {
  const { data } = await apiClient.get<GoalDto[]>('/goals', { params: { organizationId } });
  return data;
}

export async function updateGoals(organizationId: number, goals: GoalDto[]): Promise<GoalDto[]> {
  const { data } = await apiClient.put<GoalDto[]>('/goals', goals, { params: { organizationId } });
  return data;
}

export interface UserSummaryDto {
  id: number;
  name: string;
  email: string;
  role: string;
  organizationId: number | null;
  organizationName: string | null;
  branchId: number | null;
  tosAccepted: boolean;
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  user: UserSummaryDto;
}

export async function login(email: string, password: string): Promise<LoginResponse> {
  const { data } = await apiClient.post<LoginResponse>('/auth/login', { email, password });
  return data;
}

export async function logout(refreshToken: string): Promise<void> {
  await apiClient.post('/auth/logout', { refreshToken });
}

export async function forgotPassword(email: string): Promise<{ message: string; resetToken: string | null }> {
  const { data } = await apiClient.post('/auth/forgot-password', { email });
  return data;
}

export async function resetPassword(token: string, newPassword: string): Promise<void> {
  await apiClient.post('/auth/reset-password', { token, newPassword });
}

export async function acceptTos(): Promise<UserSummaryDto> {
  const { data } = await apiClient.patch<UserSummaryDto>('/auth/accept-tos');
  return data;
}

export interface UserListDto {
  id: number;
  name: string;
  email: string;
  role: string;
  branchId: number | null;
  branchName: string | null;
  active: boolean;
}

export async function fetchUsers(): Promise<UserListDto[]> {
  const { data } = await apiClient.get<UserListDto[]>('/users');
  return data;
}

export async function createUser(req: { name: string; email: string; password: string; role: string; branchId?: number }): Promise<UserListDto> {
  const { data } = await apiClient.post<UserListDto>('/users', req);
  return data;
}

export async function deactivateUser(id: number): Promise<UserListDto> {
  const { data } = await apiClient.patch<UserListDto>(`/users/${id}/deactivate`);
  return data;
}

export async function activateUser(id: number): Promise<UserListDto> {
  const { data } = await apiClient.patch<UserListDto>(`/users/${id}/activate`);
  return data;
}

export interface NotificationDto {
  id: number;
  title: string;
  message: string;
  severity: 'INFO' | 'SUCCESS' | 'WARNING';
  createdAt: string;
  readAt: string | null;
}

export async function fetchNotifications(): Promise<NotificationDto[]> {
  const { data } = await apiClient.get<NotificationDto[]>('/notifications');
  return data;
}

export async function fetchUnreadNotificationCount(): Promise<number> {
  const { data } = await apiClient.get<{ count: number }>('/notifications/unread-count');
  return data.count;
}

export async function markNotificationRead(id: number): Promise<NotificationDto> {
  const { data } = await apiClient.patch<NotificationDto>(`/notifications/${id}/read`);
  return data;
}

export interface AuditLogDto {
  id: number;
  actorEmail: string;
  action: string;
  entityType: string | null;
  entityId: string | null;
  details: string | null;
  createdAt: string;
}

export async function fetchAuditLog(): Promise<AuditLogDto[]> {
  const { data } = await apiClient.get<AuditLogDto[]>('/audit-log');
  return data;
}

export async function exportTenantData(): Promise<unknown> {
  const { data } = await apiClient.get('/data-export');
  return data;
}

export interface ItemImportRow {
  name: string;
  subCategory?: string;
  costPrice: number;
  salePrice: number;
  quantity: number;
  lastSaleDate?: string;
  expiryDate?: string;
}

export async function bulkImportItems(branchId: number, items: ItemImportRow[]): Promise<{ createdCount: number; errors: string[] }> {
  const { data } = await apiClient.post('/items/bulk', { branchId, items });
  return data;
}

export interface CreateOrganizationRequest {
  organizationName: string;
  category: string;
  branchName: string;
  branchCity?: string;
  ownerName: string;
  ownerEmail: string;
}

export interface CreateOrganizationResponse {
  organizationId: number;
  organizationName: string;
  branchId: number;
  ownerEmail: string;
  temporaryPassword: string;
}

export async function createOrganization(req: CreateOrganizationRequest): Promise<CreateOrganizationResponse> {
  const { data } = await apiClient.post<CreateOrganizationResponse>('/admin/organizations', req);
  return data;
}

export interface AdminOrganizationDto {
  id: number;
  name: string;
  category: string;
  branchCount: number;
  avgHealthScore: number;
  lastActivityDate: string | null;
}

export interface AdminStagnantItemDto {
  organizationName: string;
  branchName: string;
  itemName: string;
  quantity: number;
  inventoryValue: number;
  lastSaleDate: string | null;
}

export interface AdminPlatformTrendPointDto {
  date: string;
  totalSales: number;
}

export interface AdminCityBreakdownDto {
  city: string;
  organizationCount: number;
  branchCount: number;
  avgHealthScore: number;
}

export interface AdminRiskOpportunityDto {
  openRisksCount: number;
  openRisksValue: number;
  openOpportunitiesCount: number;
  openOpportunitiesValue: number;
}

export interface AdminHealthDistributionDto {
  good: number;
  medium: number;
  poor: number;
}

export interface AdminDecisionDto {
  id: number;
  organizationName: string;
  branchName: string;
  itemName: string;
  category: 'RISK' | 'OPPORTUNITY';
  financialImpact: number;
  confidenceScore: number;
  reasonSummary: string;
  createdAt: string;
}

export interface AdminOverviewDto {
  totalOrganizations: number;
  totalBranches: number;
  avgHealthScore: number;
  totalStagnantValue: number;
  totalSalesToday: number;
  organizationsByCategory: Record<string, number>;
  salesTrend: AdminPlatformTrendPointDto[];
  cityBreakdown: AdminCityBreakdownDto[];
  riskOpportunity: AdminRiskOpportunityDto;
  healthDistribution: AdminHealthDistributionDto;
  leaderboard: AdminOrganizationDto[];
  performanceImpactSummary: PerformanceImpactSummaryDto;
  topRecommendations: AdminDecisionDto[];
}

export async function fetchAdminOverview(): Promise<AdminOverviewDto> {
  const { data } = await apiClient.get<AdminOverviewDto>('/admin/overview');
  return data;
}

export async function fetchAdminOrganizations(): Promise<AdminOrganizationDto[]> {
  const { data } = await apiClient.get<AdminOrganizationDto[]>('/admin/organizations');
  return data;
}

export async function fetchAdminStagnantItems(): Promise<AdminStagnantItemDto[]> {
  const { data } = await apiClient.get<AdminStagnantItemDto[]>('/admin/stagnant-items/aggregate');
  return data;
}

export interface GroupOrderDto {
  id: number;
  itemName: string;
  targetQuantity: number;
  currentQuantity: number;
  estimatedMarketPrice: number;
  negotiatedPrice: number | null;
  status: 'COLLECTING' | 'NEGOTIATED' | 'DISTRIBUTED' | 'CANCELLED';
  participantCount: number;
  createdAt: string;
}

export interface GroupOrderParticipationDto {
  groupOrderId: number;
  itemName: string;
  quantity: number;
  status: string;
  estimatedMarketPrice: number;
  negotiatedPrice: number | null;
  savings: number | null;
}

export async function fetchOpenGroupOrders(): Promise<GroupOrderDto[]> {
  const { data } = await apiClient.get<GroupOrderDto[]>('/group-orders/open');
  return data;
}

export async function joinGroupOrder(id: number, quantity: number): Promise<GroupOrderDto> {
  const { data } = await apiClient.post<GroupOrderDto>(`/group-orders/${id}/join`, { quantity });
  return data;
}

export async function fetchMyGroupOrderParticipation(): Promise<GroupOrderParticipationDto[]> {
  const { data } = await apiClient.get<GroupOrderParticipationDto[]>('/group-orders/my-participation');
  return data;
}

export async function fetchAdminGroupOrders(): Promise<GroupOrderDto[]> {
  const { data } = await apiClient.get<GroupOrderDto[]>('/admin/group-orders');
  return data;
}

export async function createAdminGroupOrder(req: { itemName: string; targetQuantity: number; estimatedMarketPrice: number }): Promise<GroupOrderDto> {
  const { data } = await apiClient.post<GroupOrderDto>('/admin/group-orders', req);
  return data;
}

export async function negotiateGroupOrder(id: number, negotiatedPrice: number): Promise<GroupOrderDto> {
  const { data } = await apiClient.patch<GroupOrderDto>(`/admin/group-orders/${id}/negotiate`, { negotiatedPrice });
  return data;
}

export async function distributeGroupOrder(id: number): Promise<GroupOrderDto> {
  const { data } = await apiClient.patch<GroupOrderDto>(`/admin/group-orders/${id}/distribute`);
  return data;
}

export interface CategoryBenchmarkDto {
  category: string;
  targetMarginPercent: number;
  liquidityRatioMin: number;
  liquidityRatioMax: number;
  inventoryCoverageMinMonths: number;
  inventoryCoverageMaxMonths: number;
  stagnationDaysThreshold: number;
  slowMovingDaysThreshold: number;
  mediumMovingDaysThreshold: number;
  weightSales: number;
  weightProfit: number;
  weightPricing: number;
  weightPurchases: number;
  weightInventory: number;
  weightLiquidity: number;
}

export async function fetchAdminBenchmarks(): Promise<CategoryBenchmarkDto[]> {
  const { data } = await apiClient.get<CategoryBenchmarkDto[]>('/admin/benchmarks');
  return data;
}

export async function updateAdminBenchmark(category: string, req: Omit<CategoryBenchmarkDto, 'category'>): Promise<CategoryBenchmarkDto> {
  const { data } = await apiClient.put<CategoryBenchmarkDto>(`/admin/benchmarks/${category}`, req);
  return data;
}

export async function createSupplierUser(req: { name: string; email: string }): Promise<{ userId: number; email: string; temporaryPassword: string }> {
  const { data } = await apiClient.post('/admin/supplier-users', req);
  return data;
}

export interface SupplierPortalPurchaseDto {
  id: number;
  organizationName: string;
  branchName: string;
  itemName: string | null;
  quantity: number;
  costPrice: number;
  status: 'SENT' | 'RECEIVED';
  purchaseDate: string;
  expectedDeliveryDate: string;
  receivedDate: string | null;
  supplierResponse: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  supplierPromisedDate: string | null;
  supplierRejectionReason: string | null;
}

export async function acceptSupplierOrder(purchaseId: number, promisedDate?: string) {
  const { data } = await apiClient.patch(`/supplier/orders/${purchaseId}/accept`, { promisedDate: promisedDate ?? null });
  return data;
}

export async function rejectSupplierOrder(purchaseId: number, reason: string) {
  const { data } = await apiClient.patch(`/supplier/orders/${purchaseId}/reject`, { reason });
  return data;
}

export interface SupplierPortalOverviewDto {
  supplierName: string;
  organizationsServedCount: number;
  openOrdersCount: number;
  openOrdersValue: number;
  receivedOrdersCount: number;
  totalReceivedValue: number;
  avgRating: number | null;
  recentOrders: SupplierPortalPurchaseDto[];
}

export async function fetchSupplierOverview(): Promise<SupplierPortalOverviewDto> {
  const { data } = await apiClient.get<SupplierPortalOverviewDto>('/supplier/overview');
  return data;
}
