import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api, qs } from '../api';
import type { Customer, Page, Status, Summary, Technician } from '../types';
import { STATUSES } from '../types';
import { Empty, ErrorBox, PriorityTag, SlaBar, Spinner, label, useLoad } from '../ui';

const COLORS: Record<Status, string> = { NEW: '#8da2f5', ASSIGNED: '#5fb0cc', IN_PROGRESS: '#e5a02a', ON_HOLD: '#b07ac9', COMPLETED: '#58b27e', CLOSED: '#44565f', CANCELLED: '#d98a7e' };

function Donut({ pct }: { pct: number | null }) {
  const r = 52, c = 2 * Math.PI * r, v = pct ?? 0;
  const color = pct === null ? '#c9d2ce' : pct >= 90 ? 'var(--ok)' : pct >= 75 ? 'var(--warn)' : 'var(--bad)';
  return (
    <svg viewBox="0 0 140 140" width="150" height="150" role="img" aria-label={pct === null ? 'No SLA data' : `SLA ${pct}% met`}>
      <circle cx="70" cy="70" r={r} fill="none" stroke="#e3e8e5" strokeWidth="16" />
      <circle cx="70" cy="70" r={r} fill="none" stroke={color} strokeWidth="16" strokeDasharray={`${(v / 100) * c} ${c}`} transform="rotate(-90 70 70)" strokeLinecap="round" />
      <text x="70" y="68" textAnchor="middle" fontSize="28" fontWeight="700" fontFamily="Barlow Semi Condensed">{pct === null ? '—' : `${Math.round(pct)}%`}</text>
      <text x="70" y="88" textAnchor="middle" fontSize="12" fill="#7a8b93">SLA met</text>
    </svg>
  );
}

export default function Dashboard() {
  const [days, setDays] = useState(''); const [tech, setTech] = useState(''); const [cust, setCust] = useState('');
  const [msg, setMsg] = useState('');
  const techs = useLoad(() => api<Technician[]>('/api/users/technicians'), []);
  const custs = useLoad(() => api<Page<Customer>>('/api/customers?size=100'), []);
  const { data: s, error, loading, reload } = useLoad(
    () => api<Summary>('/api/reports/summary' + qs({ days, technicianId: tech, customerId: cust })), [days, tech, cust]);

  const scan = async () => { const r = await api<{ flagged: number }>('/api/sla/scan', 'POST'); setMsg(`SLA check done: ${r.flagged} order(s) newly flagged.`); reload(); };
  const max = s ? Math.max(1, ...STATUSES.map(x => s.statusCounts[x] ?? 0)) : 1;

  return (
    <>
      <div className="page-h"><h1>Operations dashboard</h1><button className="btn ghost" onClick={scan}>Run SLA check now</button></div>
      <div className="filters">
        <select value={days} onChange={e => setDays(e.target.value)} aria-label="Period"><option value="">All time</option><option value="7">Last 7 days</option><option value="30">Last 30 days</option><option value="90">Last 90 days</option></select>
        <select value={tech} onChange={e => setTech(e.target.value)} aria-label="Technician"><option value="">All technicians</option>{techs.data?.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}</select>
        <select value={cust} onChange={e => setCust(e.target.value)} aria-label="Customer"><option value="">All customers</option>{custs.data?.content.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select>
      </div>
      {msg && <div className="alert ok">{msg}</div>}
      <ErrorBox err={error} />
      {loading && !s && <Spinner />}
      {s && (
        <div className="stack" style={{ opacity: loading ? .6 : 1 }}>
          <div className="grid g4">
            <div className="card kpi"><span className="n">{s.open}</span><span className="l">Open work orders</span></div>
            <div className={`card kpi ${s.overdue ? 'bad' : ''}`}><span className="n">{s.overdue}</span><span className="l">Overdue (SLA breached)</span></div>
            <div className={`card kpi ${s.atRisk ? 'warn' : ''}`}><span className="n">{s.atRisk}</span><span className="l">At risk of breach</span></div>
            <div className="card kpi"><span className="n">{s.total}</span><span className="l">Total in view</span></div>
          </div>
          {s.total === 0 ? <Empty title="No work orders match these filters" hint="Try a wider period or clear a filter." /> : (<>
            <div className="grid g2" style={{ gridTemplateColumns: '2fr 1fr' }}>
              <div className="card"><h2>Work orders by status</h2>
                <div className="bars">{STATUSES.map(st => { const n = s.statusCounts[st] ?? 0; return (
                  <div className="bar" key={st}><b>{n}</b><div style={{ height: `${(n / max) * 100}%`, background: COLORS[st] }} /><span className="muted">{label(st)}</span></div>); })}</div></div>
              <div className="card center" style={{ flexDirection: 'column' }}><h2>SLA compliance</h2><Donut pct={s.slaCompliance.percent} />
                <span className="muted small">{s.slaCompliance.total === 0 ? 'No completed jobs in this period' : `${s.slaCompliance.met} of ${s.slaCompliance.total} completed on time`}</span></div>
            </div>
            <div className="grid g2">
              <div className="card"><h2>By technician</h2><table><thead><tr><th>Technician</th><th className="n">Open</th><th className="n">Done</th><th className="n">Overdue</th></tr></thead><tbody>
                {s.byTechnician.map(t => <tr key={t.technicianId}><td>{t.name}</td><td className="n">{t.open}</td><td className="n">{t.completed}</td><td className="n" style={{ color: t.overdue ? 'var(--bad)' : undefined }}>{t.overdue}</td></tr>)}</tbody></table></div>
              <div className="card"><h2>By site</h2><table><thead><tr><th>Site</th><th className="n">Total</th><th className="n">Open</th><th className="n">Overdue</th></tr></thead><tbody>
                {s.bySite.map(t => <tr key={t.siteId}><td>{t.name}</td><td className="n">{t.total}</td><td className="n">{t.open}</td><td className="n" style={{ color: t.overdue ? 'var(--bad)' : undefined }}>{t.overdue}</td></tr>)}</tbody></table></div>
            </div>
            <div className="card list"><div style={{ padding: '14px 16px 0' }}><h2>Overdue work</h2></div>
              {s.overdueOrders.length === 0 ? <Empty title="Nothing overdue 🎉" /> : s.overdueOrders.map(w => (
                <Link key={w.id} to={`/work-orders/${w.id}`} className="wo"><div className="row between"><div><span className="code">{w.code}</span> <PriorityTag p={w.priority} /><div className="t">{w.title}</div>
                  <span className="muted small">{w.customer.name} · {w.site.name} · {w.assignee?.name ?? 'Unassigned'}</span></div>
                  <SlaBar status={w.slaStatus} pct={w.slaPercentElapsed} due={w.slaDueAt} /></div></Link>))}
            </div>
          </>)}
        </div>
      )}
    </>
  );
}
