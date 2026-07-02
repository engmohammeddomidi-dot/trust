import { Navigate, Outlet } from 'react-router-dom';
import { getSession } from './session';

export function RequireAuth({ requireAdmin }: { requireAdmin?: boolean }) {
  const session = getSession();
  if (!session) return <Navigate to="/login" replace />;
  if (requireAdmin && session.role !== 'PLATFORM_ADMIN') return <Navigate to="/" replace />;
  if (!requireAdmin && session.role === 'PLATFORM_ADMIN') return <Navigate to="/admin" replace />;
  return <Outlet />;
}
