import { useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, qs } from '../api';
import type { Customer, Page, Site } from '../types';
import { Empty, ErrorBox, Modal, Pager, Spinner, useDebounced, useLoad } from '../ui';

function CustomerModal({ initial, onClose, onSaved }: { initial?: Customer; onClose: () => void; onSaved: () => void }) {
  const [f, setF] = useState({ name: initial?.name ?? '', contactName: initial?.contactName ?? '', contactEmail: initial?.contactEmail ?? '', contactPhone: initial?.contactPhone ?? '', address: initial?.address ?? '', active: initial?.active ?? true });
  const [err, setErr] = useState<unknown>(null);
  const set = (k: string) => (e: { target: { value: string } }) => setF({ ...f, [k]: e.target.value });
  const save = async (e: FormEvent) => { e.preventDefault(); try { await api(initial ? `/api/customers/${initial.id}` : '/api/customers', initial ? 'PUT' : 'POST', f); onSaved(); } catch (x) { setErr(x); } };
  return (
    <Modal title={initial ? 'Edit customer' : 'New customer'} onClose={onClose}><form onSubmit={save}>
      <div className="field"><label>Organisation name</label><input required value={f.name} onChange={set('name')} /></div>
      <div className="grid g2"><div className="field"><label>Contact name</label><input value={f.contactName} onChange={set('contactName')} /></div><div className="field"><label>Contact phone</label><input value={f.contactPhone} onChange={set('contactPhone')} /></div></div>
      <div className="field"><label>Contact email</label><input type="email" value={f.contactEmail} onChange={set('contactEmail')} /></div>
      <div className="field"><label>Address</label><input value={f.address} onChange={set('address')} /></div>
      {initial && <div className="field"><label><input type="checkbox" style={{ width: 'auto' }} checked={f.active} onChange={e => setF({ ...f, active: e.target.checked })} /> Active</label></div>}
      <ErrorBox err={err} /><button className="btn">Save</button></form></Modal>
  );
}

export function Customers() {
  const [q, setQ] = useState(''); const [page, setPage] = useState(0); const [adding, setAdding] = useState(false);
  const dq = useDebounced(q);
  const { data, error, loading, reload } = useLoad(() => api<Page<Customer>>('/api/customers' + qs({ q: dq, page, size: 12 })), [dq, page]);
  return (
    <>
      <div className="page-h"><h1>Customers</h1><button className="btn" onClick={() => setAdding(true)}>+ New customer</button></div>
      <div className="filters"><input placeholder="Search customers…" value={q} onChange={e => { setQ(e.target.value); setPage(0); }} aria-label="Search customers" /></div>
      <ErrorBox err={error} />{loading && !data && <Spinner />}
      {data && (data.content.length === 0 ? <div className="card"><Empty title="No customers found" /></div> : (
        <div className="card list">{data.content.map(c => (
          <Link key={c.id} to={`/customers/${c.id}`} className="wo"><div className="row between"><div><div className="t">{c.name} {!c.active && <span className="pill">Inactive</span>}</div>
            <span className="muted small">{c.contactName ?? '—'} · {c.contactEmail ?? '—'}</span></div><span className="muted small">{c.siteCount} site{c.siteCount === 1 ? '' : 's'}</span></div></Link>))}</div>))}
      {data && <Pager page={data} onPage={setPage} />}
      {adding && <CustomerModal onClose={() => setAdding(false)} onSaved={() => { setAdding(false); reload(); }} />}
    </>
  );
}

function SiteModal({ customerId, initial, onClose, onSaved }: { customerId: number; initial?: Site; onClose: () => void; onSaved: () => void }) {
  const [f, setF] = useState({ name: initial?.name ?? '', addressLine: initial?.addressLine ?? '', city: initial?.city ?? '', state: initial?.state ?? '', postalCode: initial?.postalCode ?? '', country: initial?.country ?? '' });
  const [err, setErr] = useState<unknown>(null);
  const set = (k: string) => (e: { target: { value: string } }) => setF({ ...f, [k]: e.target.value });
  const save = async (e: FormEvent) => { e.preventDefault(); try { await api(initial ? `/api/sites/${initial.id}` : `/api/customers/${customerId}/sites`, initial ? 'PUT' : 'POST', f); onSaved(); } catch (x) { setErr(x); } };
  return (
    <Modal title={initial ? 'Edit site' : 'New site'} onClose={onClose}><form onSubmit={save}>
      <div className="field"><label>Site name</label><input required value={f.name} onChange={set('name')} /></div>
      <div className="field"><label>Address</label><input required value={f.addressLine} onChange={set('addressLine')} /></div>
      <div className="grid g2"><div className="field"><label>City</label><input required value={f.city} onChange={set('city')} /></div><div className="field"><label>State</label><input value={f.state} onChange={set('state')} /></div>
        <div className="field"><label>Postal code</label><input value={f.postalCode} onChange={set('postalCode')} /></div><div className="field"><label>Country</label><input value={f.country} onChange={set('country')} /></div></div>
      <ErrorBox err={err} /><button className="btn">Save</button></form></Modal>
  );
}

export function CustomerDetail() {
  const { id } = useParams(); const [page, setPage] = useState(0); const [q, setQ] = useState('');
  const [editC, setEditC] = useState(false); const [siteModal, setSiteModal] = useState<Site | 'new' | null>(null);
  const dq = useDebounced(q);
  const c = useLoad(() => api<Customer>(`/api/customers/${id}`), [id]);
  const sites = useLoad(() => api<Page<Site>>(`/api/customers/${id}/sites` + qs({ q: dq, page, size: 10 })), [id, dq, page]);
  if (c.loading && !c.data) return <Spinner />;
  if (!c.data) return <ErrorBox err={c.error} />;
  return (
    <>
      <div className="page-h"><div><Link to="/customers" className="small">← Customers</Link><h1>{c.data.name}</h1><span className="muted">{c.data.contactName} · {c.data.contactEmail} · {c.data.contactPhone}</span></div>
        <button className="btn ghost" onClick={() => setEditC(true)}>Edit customer</button></div>
      <div className="row between" style={{ marginBottom: 10 }}><h2>Sites</h2><button className="btn" onClick={() => setSiteModal('new')}>+ Add site</button></div>
      <div className="filters"><input placeholder="Search sites…" value={q} onChange={e => { setQ(e.target.value); setPage(0); }} /></div>
      <ErrorBox err={sites.error} />
      {sites.data && (sites.data.content.length === 0 ? <div className="card"><Empty title="No sites yet" hint="Add the first building for this customer." /></div> : (
        <div className="card list">{sites.data.content.map(s => (
          <div key={s.id} className="wo"><div className="row between"><div><div className="t">{s.name}</div><span className="muted small">{s.addressLine}, {s.city} {s.state ?? ''} {s.postalCode ?? ''}</span></div>
            <button className="btn ghost sm" onClick={() => setSiteModal(s)}>Edit</button></div></div>))}</div>))}
      {sites.data && <Pager page={sites.data} onPage={setPage} />}
      {editC && <CustomerModal initial={c.data} onClose={() => setEditC(false)} onSaved={() => { setEditC(false); c.reload(); }} />}
      {siteModal && <SiteModal customerId={c.data.id} initial={siteModal === 'new' ? undefined : siteModal} onClose={() => setSiteModal(null)} onSaved={() => { setSiteModal(null); sites.reload(); c.reload(); }} />}
    </>
  );
}
