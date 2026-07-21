import { useState } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { TosGateModal } from '../components/TosGateModal';
import { getSession } from './session';

export function RequireAuth({ requireAdmin, requireSupplier }: { requireAdmin?: boolean; requireSupplier?: boolean }) {
  const session = getSession();
  const [tosAccepted, setTosAccepted] = useState(session?.tosAccepted ?? true);

  if (!session) return <Navigate to="/login" replace />;
  if (requireAdmin && session.role !== 'PLATFORM_ADMIN') return <Navigate to="/" replace />;
  if (requireSupplier && session.role !== 'SUPPLIER') return <Navigate to="/" replace />;
  if (!requireAdmin && !requireSupplier && session.role === 'PLATFORM_ADMIN') return <Navigate to="/admin" replace />;
  if (!requireAdmin && !requireSupplier && session.role === 'SUPPLIER') return <Navigate to="/supplier" replace />;

  if (!tosAccepted) {
    return <TosGateModal onAccepted={() => setTosAccepted(true)} />;
  }

  return <Outlet />;
}
