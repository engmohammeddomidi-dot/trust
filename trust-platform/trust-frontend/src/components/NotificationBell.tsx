import { useEffect, useRef, useState } from 'react';
import { fetchNotifications, fetchUnreadNotificationCount, markNotificationRead, type NotificationDto } from '../api/client';
import { Icon } from './Icon';

const severityColor: Record<string, string> = {
  INFO: 'var(--accent-blue)',
  SUCCESS: 'var(--accent-green)',
  WARNING: 'var(--accent-amber)',
};

export function NotificationBell() {
  const [open, setOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState<NotificationDto[] | null>(null);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    fetchUnreadNotificationCount().then(setUnreadCount).catch(() => {});
  }, []);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  function toggleOpen() {
    if (!open) {
      fetchNotifications().then(setNotifications);
    }
    setOpen((v) => !v);
  }

  async function handleMarkRead(id: number) {
    await markNotificationRead(id);
    setNotifications((prev) => prev?.map((n) => (n.id === id ? { ...n, readAt: new Date().toISOString() } : n)) ?? null);
    setUnreadCount((c) => Math.max(0, c - 1));
  }

  return (
    <div ref={ref} style={{ position: 'relative' }}>
      <button
        className="notification-bell-btn"
        onClick={toggleOpen}
        aria-label={unreadCount > 0 ? `التنبيهات، ${unreadCount} غير مقروءة` : 'التنبيهات'}
        aria-expanded={open}
      >
        <Icon name="notifications" size={17} />
        {unreadCount > 0 && (
          <span style={{
            position: 'absolute', top: -4, insetInlineStart: -4, background: 'var(--accent-red)', color: 'white',
            fontSize: 10, borderRadius: 10, padding: '1px 5px', minWidth: 16, textAlign: 'center',
          }}>
            {unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div style={{
          position: 'absolute', top: 46, insetInlineEnd: 0, width: 320, maxHeight: 400, overflowY: 'auto',
          background: 'var(--bg-panel)', border: '1px solid var(--border-subtle)', borderRadius: 'var(--radius-lg)',
          padding: 10, zIndex: 300, boxShadow: '0 8px 24px rgba(0,0,0,0.4)',
        }}>
          <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 8, padding: '0 6px' }}>التنبيهات</div>
          {notifications === null && <p style={{ fontSize: 12, color: 'var(--text-secondary)', padding: '0 6px' }}>جاري التحميل...</p>}
          {notifications !== null && notifications.length === 0 && (
            <p style={{ fontSize: 12, color: 'var(--text-secondary)', padding: '0 6px' }}>لا توجد تنبيهات بعد.</p>
          )}
          {notifications?.map((n) => (
            <div
              key={n.id}
              onClick={() => !n.readAt && handleMarkRead(n.id)}
              style={{
                padding: '8px 6px', borderBottom: '1px solid var(--border-subtle)', cursor: n.readAt ? 'default' : 'pointer',
                opacity: n.readAt ? 0.6 : 1,
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 3 }}>
                <span style={{ width: 6, height: 6, borderRadius: '50%', background: severityColor[n.severity], flexShrink: 0 }} />
                <span style={{ fontSize: 12, fontWeight: 600 }}>{n.title}</span>
              </div>
              <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>{n.message}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
