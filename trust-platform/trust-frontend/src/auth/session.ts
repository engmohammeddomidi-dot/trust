export interface Session {
  token: string;
  refreshToken: string;
  userId: number;
  name: string;
  email: string;
  role: 'OWNER' | 'BRANCH_MANAGER' | 'STAFF' | 'PLATFORM_ADMIN';
  organizationId: number | null;
  organizationName: string | null;
  branchId: number | null;
  tosAccepted: boolean;
}

const STORAGE_KEY = 'trust_session';

export function getSession(): Session | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as Session;
  } catch {
    return null;
  }
}

export function setSession(session: Session): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
}

export function updateTokens(token: string, refreshToken: string): void {
  const session = getSession();
  if (!session) return;
  setSession({ ...session, token, refreshToken });
}

export function clearSession(): void {
  localStorage.removeItem(STORAGE_KEY);
}

export function isAdmin(): boolean {
  return getSession()?.role === 'PLATFORM_ADMIN';
}

/** الفروع والمؤسسات دائمًا معروفة داخل الصفحات المحمية بـ RequireAuth غير الإدارية */
export function requireOrganizationId(): number {
  const id = getSession()?.organizationId;
  if (!id) throw new Error('لا توجد مؤسسة مرتبطة بهذا المستخدم');
  return id;
}

export function requireBranchId(): number {
  const id = getSession()?.branchId;
  if (!id) throw new Error('لا يوجد فرع مرتبط بهذا المستخدم');
  return id;
}
