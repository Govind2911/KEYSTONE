import { useEffect, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from './auth';
import { api } from './api';
import type { Notification, Page, Role } from './types';
import { fmtDate } from './ui';

const NAV: Record<Role, { to: string; label: string }[]> = {
  MANAGER: [{ to: '/dashboard', label: 'Dashboard' }, { to: '/board', label: 'Board' }, { to: '/work-orders', label: 'Work orders' },
    { to: '/customers', label: 'Customers' }, { to: '/parts', label: 'Parts' }, { to: '/users', label: 'Users' }, { to: '/audit', label: 'Audit log' }],
  DISPATCHER: [{ to: '/board', label: 'Board' }, { to: '/work-orders', label: 'Work orders' }, { to: '/customers', label: 'Customers' }],
  TECHNICIAN: [{ to: '/work-orders', label: 'My jobs' }],
  CUSTOMER: [{ to: '/work-orders', label: 'My requests' }, { to: '/work-orders/new', label: 'New request' }],
};

function Bell() {
  const nav = useNavigate();
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const [items, setItems] = useState<Notification[]>([]);
  const refresh = () => api<{ unread: number }>('/api/notifications/unread-count').then(r => setUnread(r.unread)).catch(() => {});
  useEffect(() => { refresh(); const t = setInterval(refresh, 30000); return () => clearInterval(t); }, []);
  const toggle = async () => {
    const next = !open; setOpen(next);
    if (next) setItems((await api<Page<Notification>>('/api/notifications?size=15')).content);
  };
  const click = async (n: Notification) => {
    if (!n.seen) { await api(`/api/notifications/${n.id}/read`, 'POST'); refresh(); }
    setOpen(false); if (n.workOrderId) nav(`/work-orders/${n.workOrderId}`);
  };
  const readAll = async () => { await api('/api/notifications/read-all', 'POST'); setUnread(0); setItems(i => i.map(x => ({ ...x, seen: true }))); };
  return (
    <div style={{ position: 'relative' }}>
      <button className="bell" onClick={toggle} aria-label="Notifications">🔔{unread > 0 && <span className="dot">{unread}</span>}</button>
      {open && (
        <div className="dropdown">
          <div className="row between" style={{ padding: '10px 14px' }}><b>Notifications</b>{unread > 0 && <button className="btn ghost sm" onClick={readAll}>Mark all read</button>}</div>
          {items.length === 0 && <div className="empty">You're all caught up.</div>}
          {items.map(n => (
            <div key={n.id} className={`item ${n.seen ? '' : 'unread'}`} onClick={() => click(n)}>
              <div>{n.message}</div><div className="muted small">{fmtDate(n.createdAt)}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default function Layout() {
  const { user, logout } = useAuth();
  if (!user) return null;
  return (
    <div className="shell">
      <aside className="side">
        <div className="brand"><i />KEYSTONE</div>
        <nav className="nav">
          {NAV[user.role].map(l => <NavLink key={l.to} to={l.to} end={l.to === '/work-orders'}>{l.label}</NavLink>)}
        </nav>
        <div className="me"><b>{user.name}</b>{user.role.toLowerCase()}{user.customerName ? ` · ${user.customerName}` : ''}<br />
          <a href="#" onClick={e => { e.preventDefault(); logout(); }} style={{ color: '#9fc0ff' }}>Sign out</a></div>
      </aside>
      <main className="main">
        <div className="topbar">
          <span className="muted small">{user.name} · {user.role.toLowerCase()}</span><Bell />
          <button className="btn ghost sm" onClick={logout}>Sign out</button>
        </div>
        <Outlet />
      </main>
    </div>
  );
}
