import axios from 'axios';
import { clearSession, getSession } from '../auth/session';

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
});

apiClient.interceptors.request.use((config) => {
  const session = getSession();
  if (session?.token) {
    config.headers.Authorization = `Bearer ${session.token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && !window.location.pathname.startsWith('/login')) {
      clearSession();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export interface HealthScoreDto {
  salesScore: number;
  profitScore: number;
  pricingScore: number;
  purchasesScore: number;
  inventoryScore: number;
  liquidityScore: number;
  totalScore: number;
  label: string;
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
  healthScore: HealthScoreDto;
  salesTrend: { date: string; sales: number }[];
  topRecommendations: RecommendationDto[];
  inventoryBreakdown: Record<string, number>;
  liquidityBreakdown: Record<string, number>;
  itemsNeedingAttention: ItemDto[];
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

export async function updateOrganization(id: number, name: string): Promise<OrganizationDto> {
  const { data } = await apiClient.put<OrganizationDto>(`/organizations/${id}`, { name });
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
  supplierName: string;
  quantity: number;
  costPrice: number;
  totalCost: number;
  purchaseDate: string;
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

export interface UserSummaryDto {
  id: number;
  name: string;
  email: string;
  role: string;
  organizationId: number | null;
  organizationName: string | null;
  branchId: number | null;
}

export interface LoginResponse {
  token: string;
  user: UserSummaryDto;
}

export async function login(email: string, password: string): Promise<LoginResponse> {
  const { data } = await apiClient.post<LoginResponse>('/auth/login', { email, password });
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

export interface AdminOverviewDto {
  totalOrganizations: number;
  totalBranches: number;
  avgHealthScore: number;
  totalStagnantValue: number;
  organizationsByCategory: Record<string, number>;
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
