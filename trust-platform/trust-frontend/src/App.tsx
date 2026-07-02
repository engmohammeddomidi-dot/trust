import { Routes, Route } from 'react-router-dom';
import { RequireAuth } from './auth/RequireAuth';
import { LoginPage } from './pages/LoginPage';
import { Dashboard } from './pages/Dashboard';
import { InventoryPage } from './pages/InventoryPage';
import { NotificationsPage } from './pages/NotificationsPage';
import { PricingPage } from './pages/PricingPage';
import { ProfitabilityPage } from './pages/ProfitabilityPage';
import { LiquidityPage } from './pages/LiquidityPage';
import { SalesPage } from './pages/SalesPage';
import { PurchasesPage } from './pages/PurchasesPage';
import { SuppliersPage } from './pages/SuppliersPage';
import { SettingsPage } from './pages/SettingsPage';
import { ReportsPage } from './pages/ReportsPage';
import { AdminOverviewPage } from './pages/admin/AdminOverviewPage';
import { AdminOrganizationsPage } from './pages/admin/AdminOrganizationsPage';
import { AdminStagnantItemsPage } from './pages/admin/AdminStagnantItemsPage';
import { AdminGroupOrdersPage } from './pages/admin/AdminGroupOrdersPage';
import { AdminBenchmarksPage } from './pages/admin/AdminBenchmarksPage';
import './styles/theme.css';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route element={<RequireAuth />}>
        <Route path="/" element={<Dashboard />} />
        <Route path="/inventory" element={<InventoryPage />} />
        <Route path="/notifications" element={<NotificationsPage />} />
        <Route path="/pricing" element={<PricingPage />} />
        <Route path="/profitability" element={<ProfitabilityPage />} />
        <Route path="/liquidity" element={<LiquidityPage />} />
        <Route path="/sales" element={<SalesPage />} />
        <Route path="/purchases" element={<PurchasesPage />} />
        <Route path="/suppliers" element={<SuppliersPage />} />
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="/reports" element={<ReportsPage />} />
      </Route>

      <Route element={<RequireAuth requireAdmin />}>
        <Route path="/admin" element={<AdminOverviewPage />} />
        <Route path="/admin/organizations" element={<AdminOrganizationsPage />} />
        <Route path="/admin/stagnant-items" element={<AdminStagnantItemsPage />} />
        <Route path="/admin/group-orders" element={<AdminGroupOrdersPage />} />
        <Route path="/admin/benchmarks" element={<AdminBenchmarksPage />} />
      </Route>
    </Routes>
  );
}
