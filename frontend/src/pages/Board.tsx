import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth';
import type { Status, Technician, WorkOrder } from '../types';
import { BOARD } from '../types';
import { Empty, ErrorBox, PriorityTag, SlaBar, Spinner, label, sortTechs, useLoad } from '../ui';

export default function Board() {
  const { user } = useAuth();
  const [err, setErr] = useState<unknown>(null);
  const board = useLoad(() => api<Record<Status, WorkOrder[]>>('/api/work-orders/board'), []);
  const techs = useLoad(() => api<Technician[]>('/api/users/technicians'), []);

  const assign = async (id: number, technicianId: string) => {
    if (!technicianId) return; setErr(null);
    try { await api(`/api/work-orders/${id}/assign`, 'POST', { technicianId: Number(technicianId) }); board.reload(); techs.reload(); } catch (e) { setErr(e); }
  };
  if (!user) return null;
  const total = board.data ? BOARD.reduce((n, s) => n + (board.data![s]?.length ?? 0), 0) : 0;
  return (
    <>
      <div className="page-h"><h1>Dispatch board</h1>
        <div className="row"><button className="btn ghost" onClick={board.reload}>Refresh</button><Link className="btn" to="/work-orders/new">+ New work order</Link></div></div>
      <ErrorBox err={err || board.error} />
      {board.loading && !board.data && <Spinner />}
      {board.data && total === 0 && <Empty title="No open work" hint="New work orders will appear here." />}
      {board.data && total > 0 && (
        <div className="board">
          {BOARD.map(st => (
            <div className="col" key={st}>
              <h3><span>{label(st)}</span><span>{board.data![st]?.length ?? 0}</span></h3>
              {(board.data![st] ?? []).map(w => (
                <div key={w.id} className={`bcard ${w.slaStatus === 'BREACHED' ? 'bad' : w.slaStatus === 'AT_RISK' ? 'warn' : ''}`}>
                  <div className="row between"><span className="muted small">{w.code} · {label(w.category)}</span><PriorityTag p={w.priority} /></div>
                  <Link className="t" to={`/work-orders/${w.id}`}>{w.title}</Link>
                  <div className="muted small">{w.site.name}</div>
                  <div className="muted small" style={{ margin: '4px 0 6px' }}>{w.assignee ? `👷 ${w.assignee.name}` : 'Unassigned'}</div>
                  <SlaBar status={w.slaStatus} pct={w.slaPercentElapsed} due={w.slaDueAt} />
                  {w.canAssign && (st === 'NEW' || st === 'ASSIGNED') && (
                    <select value="" onChange={e => assign(w.id, e.target.value)} aria-label={`Assign ${w.code}`}>
                      <option value="">{w.assignee ? 'Reassign…' : 'Assign to…'}</option>
                      {sortTechs(techs.data ?? [], w.category).filter(t => t.id !== w.assignee?.id).map(t => <option key={t.id} value={t.id}>{t.skillMatch ? '★ ' : ''}{t.name} ({t.openJobs} open)</option>)}
                    </select>)}
                </div>))}
            </div>))}
        </div>)}
    </>
  );
}
