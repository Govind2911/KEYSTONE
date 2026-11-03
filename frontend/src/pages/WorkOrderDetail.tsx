import { useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, download, fetchBlob, upload } from '../api';
import { useAuth, isStaff } from '../auth';
import type { Attachment, Part, Page, PartUsage, Priority, Status, Technician, TimeLog, WorkOrder } from '../types';
import { PRIORITIES } from '../types';
import { AuthImage, Empty, ErrorBox, Modal, PriorityTag, SlaBar, Spinner, StatusPill, fmtDate, fmtMinutes, fmtMoney, fmtSize, label, sortTechs, useLoad } from '../ui';

function actionLabel(from: Status, to: Status): string {
  if (to === 'IN_PROGRESS') return from === 'ASSIGNED' ? 'Start work' : from === 'ON_HOLD' ? 'Resume' : 'Reopen';
  return ({ ON_HOLD: 'Put on hold', COMPLETED: 'Mark complete', CLOSED: 'Close & sign off', CANCELLED: 'Cancel order' } as Record<string, string>)[to] ?? label(to);
}

export default function WorkOrderDetail() {
  const { id } = useParams(); const { user } = useAuth();
  const staff = !!user && isStaff(user);
  const wo = useLoad(() => api<WorkOrder>(`/api/work-orders/${id}`), [id]);
  const parts = useLoad(() => staff ? api<PartUsage[]>(`/api/work-orders/${id}/parts`) : Promise.resolve([] as PartUsage[]), [id, staff]);
  const time = useLoad(() => staff ? api<TimeLog[]>(`/api/work-orders/${id}/time`) : Promise.resolve([] as TimeLog[]), [id, staff]);
  const files = useLoad(() => api<Attachment[]>(`/api/work-orders/${id}/attachments`), [id]);
  const techs = useLoad(() => user && user.role !== 'CUSTOMER' && user.role !== 'TECHNICIAN' ? api<Technician[]>('/api/users/technicians') : Promise.resolve([] as Technician[]), [user?.role]);
  const catalogue = useLoad(() => user && (user.role === 'TECHNICIAN' || user.role === 'MANAGER') ? api<Page<Part>>('/api/parts?size=100') : Promise.resolve(null), [user?.role]);

  const [err, setErr] = useState<unknown>(null); const [busy, setBusy] = useState(false);
  const [pending, setPending] = useState<Status | null>(null); const [note, setNote] = useState('');
  const [techId, setTechId] = useState(''); const [partId, setPartId] = useState(''); const [qty, setQty] = useState('1'); const [partNote, setPartNote] = useState('');
  const [mins, setMins] = useState(''); const [timeNote, setTimeNote] = useState(''); const [editing, setEditing] = useState(false); const [notice, setNotice] = useState('');

  if (!user) return null;
  if (wo.loading && !wo.data) return <Spinner />;
  if (wo.error || !wo.data) return <><ErrorBox err={wo.error} /><Link to="/work-orders">← Back to work orders</Link></>;
  const w = wo.data;
  const refreshAll = () => { wo.reload(); parts.reload(); time.reload(); files.reload(); techs.reload(); catalogue.reload(); };
  const run = async (fn: () => Promise<unknown>) => { setBusy(true); setErr(null); try { await fn(); refreshAll(); } catch (e) { setErr(e); } finally { setBusy(false); } };

  const canLog = (user.role === 'MANAGER' || (user.role === 'TECHNICIAN' && w.assignee?.id === user.id)) && ['IN_PROGRESS', 'ON_HOLD', 'COMPLETED'].includes(w.status);
  const canUpload = !['CLOSED', 'CANCELLED'].includes(w.status);
  const transition = () => run(async () => { await api(`/api/work-orders/${w.id}/status`, 'POST', { toStatus: pending, note }); setPending(null); setNote(''); });
  const assign = () => run(async () => { await api(`/api/work-orders/${w.id}/assign`, 'POST', { technicianId: Number(techId) }); setTechId(''); });
  const addPart = (e: FormEvent) => { e.preventDefault(); run(async () => { await api(`/api/work-orders/${w.id}/parts`, 'POST', { partId: Number(partId), quantity: Number(qty), note: partNote }); setPartId(''); setQty('1'); setPartNote(''); }); };
  const addTime = (e: FormEvent) => { e.preventDefault(); run(async () => { await api(`/api/work-orders/${w.id}/time`, 'POST', { minutes: Number(mins), note: timeNote }); setMins(''); setTimeNote(''); }); };
  const pickFile = (f?: File) => f && run(() => upload(`/api/work-orders/${w.id}/attachments`, f));
  const openFile = async (a: Attachment) => {
    const win = window.open('', '_blank');
    try { const url = URL.createObjectURL(await fetchBlob(a.url)); if (win) win.location.href = url; } catch (e) { win?.close(); setErr(e); }
  };
  const copyLink = async (a: Attachment) => {
    const link = a.publicUrl ?? window.location.origin + a.url;
    try { await navigator.clipboard.writeText(link); setNotice(a.publicUrl ? 'Cloudinary link copied.' : 'Link copied (works for signed-in users).'); setTimeout(() => setNotice(''), 3000); } catch { setNotice(link); }
  };
  const removeFile = (a: Attachment) => { if (window.confirm(`Delete ${a.fileName}?`)) run(() => api(`/api/attachments/${a.id}`, 'DELETE')); };
  const chosenPart = catalogue.data?.content.find(p => String(p.id) === partId);

  return (
    <>
      <div className="page-h">
        <div><Link to="/work-orders" className="small">← Back</Link>
          <h1><span className="muted">{w.code}</span> {w.title}</h1>
          <div className="row"><StatusPill s={w.status} /><PriorityTag p={w.priority} /><span className="pill">{label(w.category)}</span><SlaBar status={w.slaStatus} pct={w.slaPercentElapsed} due={w.slaDueAt} /></div></div>
        {w.canEdit && <button className="btn ghost" onClick={() => setEditing(true)}>Edit</button>}
      </div>
      <ErrorBox err={err} />
      {notice && <div className="alert ok">{notice}</div>}

      {w.allowedTransitions && w.allowedTransitions.length > 0 && (
        <div className="card" style={{ marginBottom: 16 }}>
          <div className="row">{w.allowedTransitions.map(t => (
            <button key={t} className={`btn ${t === 'CANCELLED' ? 'danger' : ''}`} disabled={busy} onClick={() => { setPending(t); setNote(''); }}>{actionLabel(w.status, t)}</button>))}</div>
          {pending && (
            <div style={{ marginTop: 12 }}><label htmlFor="n">Note (optional)</label>
              <textarea id="n" style={{ minHeight: 60 }} value={note} onChange={e => setNote(e.target.value)} maxLength={500} />
              <div className="row" style={{ marginTop: 8 }}><button className="btn" disabled={busy} onClick={transition}>Confirm: {actionLabel(w.status, pending)}</button><button className="btn ghost" onClick={() => setPending(null)}>Back</button></div></div>)}
        </div>)}

      <div className="detail-grid">
        <div className="stack">
          <div className="card"><h2>Details</h2><p style={{ whiteSpace: 'pre-wrap', marginTop: 0 }}>{w.description}</p>
            <div className="grid g2 small">
              <div><span className="muted">Customer</span><br />{w.customer.name}</div><div><span className="muted">Site</span><br />{w.site.name}<br /><span className="muted">{w.siteAddress}</span></div>
              {staff && <div><span className="muted">Assigned to</span><br />{w.assignee?.name ?? 'Unassigned'}</div>}
              {w.createdBy && <div><span className="muted">Raised by</span><br />{w.createdBy.name}</div>}
              <div><span className="muted">Created</span><br />{fmtDate(w.createdAt)}</div>
              {w.slaDueAt && <div><span className="muted">SLA due</span><br />{fmtDate(w.slaDueAt)}</div>}
              {w.startedAt && <div><span className="muted">Started</span><br />{fmtDate(w.startedAt)}</div>}
              {w.completedAt && <div><span className="muted">Completed</span><br />{fmtDate(w.completedAt)}</div>}
            </div></div>

          {staff && (
            <div className="card"><h2>Parts used {w.partsCost !== undefined && <span className="muted small"> · total {fmtMoney(w.partsCost)}</span>}</h2>
              {parts.data && parts.data.length === 0 ? <p className="muted">No parts logged yet.</p> : (
                <table><thead><tr><th>Part</th><th className="n">Qty</th><th className="n">Cost</th><th>By</th></tr></thead><tbody>
                  {parts.data?.map(p => <tr key={p.id}><td>{p.partName}<br /><span className="muted small">{p.sku}</span></td><td className="n">{p.quantity}</td><td className="n">{fmtMoney(p.lineCost)}</td><td>{p.usedBy}</td></tr>)}</tbody></table>)}
              {canLog && (
                <form onSubmit={addPart} className="row" style={{ marginTop: 12, alignItems: 'flex-end' }}>
                  <div className="grow" style={{ minWidth: 200 }}><label htmlFor="pp">Part</label><select id="pp" required value={partId} onChange={e => setPartId(e.target.value)}><option value="">Select…</option>
                    {catalogue.data?.content.map(p => <option key={p.id} value={p.id} disabled={p.stockQty === 0}>{p.name} ({p.stockQty} in stock)</option>)}</select></div>
                  <div style={{ width: 90 }}><label htmlFor="pq">Qty</label><input id="pq" type="number" min={1} max={chosenPart?.stockQty} required value={qty} onChange={e => setQty(e.target.value)} /></div>
                  <button className="btn" disabled={busy || !partId}>Log part</button>
                </form>)}
            </div>)}

          {staff && (
            <div className="card"><h2>Time logged {w.labourMinutes !== undefined && <span className="muted small"> · total {fmtMinutes(w.labourMinutes)}</span>}</h2>
              {time.data && time.data.length === 0 ? <p className="muted">No time logged yet.</p> : time.data?.map(t => (
                <div key={t.id} className="row between small" style={{ padding: '6px 0', borderBottom: '1px solid var(--line)' }}><span><b>{fmtMinutes(t.minutes)}</b> · {t.technicianName}{t.note ? ` — ${t.note}` : ''}</span><span className="muted">{fmtDate(t.loggedAt)}</span></div>))}
              {canLog && (
                <form onSubmit={addTime} className="row" style={{ marginTop: 12, alignItems: 'flex-end' }}>
                  <div style={{ width: 110 }}><label htmlFor="tm">Minutes</label><input id="tm" type="number" min={1} max={1440} required value={mins} onChange={e => setMins(e.target.value)} /></div>
                  <div className="grow" style={{ minWidth: 160 }}><label htmlFor="tn">Note</label><input id="tn" maxLength={500} value={timeNote} onChange={e => setTimeNote(e.target.value)} /></div>
                  <button className="btn" disabled={busy}>Log time</button>
                </form>)}
            </div>)}

          <div className="card"><h2>Photos & documents</h2>
            {files.data && files.data.length === 0 ? <p className="muted">No files yet.</p> : (
              <div className="files">{files.data?.map(a => (
                <div key={a.id} className="file">
                  {a.contentType.startsWith('image/') ? <AuthImage path={a.url} alt={a.fileName} onClick={() => openFile(a)} />
                    : <div className="thumb doc" onClick={() => openFile(a)}>{a.contentType === 'application/pdf' ? 'PDF' : 'TXT'}</div>}
                  <div className="finfo"><b title={a.fileName}>{a.fileName}</b>
                    <span className="muted small">{fmtSize(a.sizeBytes)} · {a.uploadedBy} · {a.storage === 'CLOUDINARY' ? '☁ Cloudinary' : 'Server'}</span>
                    <div className="row" style={{ gap: 6 }}>
                      <button className="btn ghost sm" onClick={() => openFile(a)}>View</button>
                      <button className="btn ghost sm" onClick={() => download(a.downloadUrl, a.fileName).catch(setErr)}>Download</button>
                      <button className="btn ghost sm" onClick={() => copyLink(a)}>Copy link</button>
                      {canUpload && (user.role === 'MANAGER' || a.uploadedById === user.id) && <button className="btn ghost sm" onClick={() => removeFile(a)}>Delete</button>}
                    </div></div>
                </div>))}</div>)}
            {canUpload && <div style={{ marginTop: 10 }}><label htmlFor="f">Add a photo or PDF (max 10 MB)</label><input id="f" type="file" accept="image/*,application/pdf,text/plain" disabled={busy} onChange={e => { pickFile(e.target.files?.[0]); e.target.value = ''; }} /></div>}
          </div>
        </div>

        <div className="stack">
          {w.canAssign && (
            <div className="card"><h3>{w.assignee ? 'Reassign technician' : 'Assign technician'}</h3>
              <select value={techId} onChange={e => setTechId(e.target.value)} aria-label="Technician"><option value="">Select…</option>
                {sortTechs(techs.data ?? [], w.category).filter(t => t.id !== w.assignee?.id).map(t => <option key={t.id} value={t.id}>{t.skillMatch ? '★ ' : ''}{t.name} — {t.openJobs} open{t.skills ? ` · ${t.skills.split(',').join('/')}` : ''}</option>)}</select>
              <button className="btn" style={{ marginTop: 10, width: '100%' }} disabled={!techId || busy} onClick={assign}>{w.assignee ? 'Reassign' : 'Assign'}</button></div>)}
          <div className="card"><h3>History</h3>
            {w.history && w.history.length > 0 ? (
              <ul className="tl">{w.history.map(h => (
                <li key={h.id}><b>{h.fromStatus && h.fromStatus === h.toStatus ? 'Reassigned' : label(h.toStatus)}</b>
                  <div className="muted small">{fmtDate(h.changedAt)}{h.changedBy ? ` · ${h.changedBy}` : ''}</div>{h.note && <div className="small">{h.note}</div>}</li>))}</ul>
            ) : <Empty title="No history yet" />}
          </div>
        </div>
      </div>
      {editing && <EditModal w={w} onClose={() => setEditing(false)} onSaved={() => { setEditing(false); refreshAll(); }} />}
    </>
  );
}

function EditModal({ w, onClose, onSaved }: { w: WorkOrder; onClose: () => void; onSaved: () => void }) {
  const [title, setTitle] = useState(w.title); const [description, setDescription] = useState(w.description); const [priority, setPriority] = useState<Priority>(w.priority);
  const [err, setErr] = useState<unknown>(null);
  const save = async (e: FormEvent) => { e.preventDefault(); try { await api(`/api/work-orders/${w.id}`, 'PUT', { title, description, priority, scheduledAt: w.scheduledAt }); onSaved(); } catch (x) { setErr(x); } };
  return (
    <Modal title={`Edit ${w.code}`} onClose={onClose}><form onSubmit={save}>
      <div className="field"><label>Title</label><input required maxLength={150} value={title} onChange={e => setTitle(e.target.value)} /></div>
      <div className="field"><label>Description</label><textarea required maxLength={3000} value={description} onChange={e => setDescription(e.target.value)} /></div>
      <div className="field"><label>Priority (changing it recalculates the SLA)</label><select value={priority} onChange={e => setPriority(e.target.value as Priority)}>{PRIORITIES.map(p => <option key={p}>{p}</option>)}</select></div>
      <ErrorBox err={err} /><button className="btn">Save changes</button></form></Modal>
  );
}
