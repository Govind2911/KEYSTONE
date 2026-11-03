import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api, qs } from '../api';
import { useAuth } from '../auth';
import type { Page, WorkOrder } from '../types';
import { PRIORITIES, STATUSES } from '../types';
import { Empty, ErrorBox, Pager, PriorityTag, SlaBar, Spinner, StatusPill, fmtDate, label, useDebounced, useLoad } from '../ui';

export default function WorkOrders() {
  const { user } = useAuth();
  const [q, setQ] = useState(''); const [status, setStatus] = useState(user?.role === 'TECHNICIAN' ? 'ACTIVE' : '');
  const [priority, setPriority] = useState(''); const [sort, setSort] = useState('createdAt,desc');
  const [overdue, setOverdue] = useState(false); const [page, setPage] = useState(0);
  const dq = useDebounced(q);
  const statusParam = status === 'ACTIVE' ? 'NEW,ASSIGNED,IN_PROGRESS,ON_HOLD' : status;
  const { data, error, loading } = useLoad(
    () => api<Page<WorkOrder>>('/api/work-orders' + qs({ q: dq, status: statusParam, priority, overdue, sort, page, size: 15 })),
    [dq, statusParam, priority, overdue, sort, page]);
  if (!user) return null;
  const title = user.role === 'TECHNICIAN' ? 'My jobs' : user.role === 'CUSTOMER' ? 'My requests' : 'Work orders';
  const canCreate = user.role !== 'TECHNICIAN';
  const f = (fn: (v: string) => void) => (e: { target: { value: string } }) => { fn(e.target.value); setPage(0); };
  return (
    <>
      <div className="page-h"><h1>{title}</h1>{canCreate && <Link className="btn" to="/work-orders/new">{user.role === 'CUSTOMER' ? '+ New request' : '+ New work order'}</Link>}</div>
      <div className="filters">
        <input placeholder="Search code, title or customer…" value={q} onChange={f(setQ)} aria-label="Search" />
        <select value={status} onChange={f(setStatus)} aria-label="Status"><option value="">All statuses</option><option value="ACTIVE">Active only</option>{STATUSES.map(s => <option key={s} value={s}>{label(s)}</option>)}</select>
        <select value={priority} onChange={f(setPriority)} aria-label="Priority"><option value="">Any priority</option>{PRIORITIES.map(p => <option key={p} value={p}>{p}</option>)}</select>
        {user.role !== 'CUSTOMER' && <select value={sort} onChange={f(setSort)} aria-label="Sort"><option value="createdAt,desc">Newest first</option><option value="slaDueAt,asc">SLA soonest</option></select>}
        {user.role !== 'CUSTOMER' && <label style={{ display: 'flex', gap: 6, alignItems: 'center', margin: 0 }}><input type="checkbox" style={{ width: 'auto' }} checked={overdue} onChange={e => { setOverdue(e.target.checked); setPage(0); }} />Overdue only</label>}
      </div>
      <ErrorBox err={error} />
      {loading && !data && <Spinner />}
      {data && (
        <div style={{ opacity: loading ? .6 : 1 }}>
          {data.content.length === 0 ? <div className="card"><Empty title="No work orders found" hint="Adjust the filters or create a new one." /></div> : (
            <div className="card list">
              {data.content.map(w => (
                <Link key={w.id} to={`/work-orders/${w.id}`} className="wo">
                  <div className="row between">
                    <div className="grow"><span className="code">{w.code}</span> <PriorityTag p={w.priority} /> <StatusPill s={w.status} />
                      <div className="t">{w.title}</div>
                      <span className="muted small">{w.customer.name} · {w.site.name}{w.assignee ? ` · 👷 ${w.assignee.name}` : user.role !== 'CUSTOMER' ? ' · Unassigned' : ''} · {fmtDate(w.createdAt)}</span></div>
                    <SlaBar status={w.slaStatus} pct={w.slaPercentElapsed} due={w.slaDueAt} />
                  </div>
                </Link>))}
            </div>)}
          <Pager page={data} onPage={setPage} />
        </div>)}
    </>
  );
}
