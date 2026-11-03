import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth';
import type { Category, Customer, Page, Priority, Site, WorkOrder } from '../types';
import { CATEGORIES, PRIORITIES } from '../types';
import { label } from '../ui';
import { ErrorBox } from '../ui';

export default function WorkOrderForm() {
  const { user } = useAuth(); const nav = useNavigate();
  const isCustomer = user?.role === 'CUSTOMER';
  const [customers, setCustomers] = useState<Customer[]>([]); const [sites, setSites] = useState<Site[]>([]);
  const [customerId, setCustomerId] = useState<string>(isCustomer ? String(user?.customerId ?? '') : '');
  const [siteId, setSiteId] = useState(''); const [title, setTitle] = useState(''); const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<Priority>('MEDIUM'); const [category, setCategory] = useState<Category>('GENERAL'); const [err, setErr] = useState<unknown>(null); const [busy, setBusy] = useState(false);

  useEffect(() => { if (!isCustomer) api<Page<Customer>>('/api/customers?size=100').then(p => setCustomers(p.content.filter(c => c.active))).catch(setErr); }, [isCustomer]);
  useEffect(() => {
    setSiteId(''); setSites([]);
    if (customerId) api<Page<Site>>(`/api/customers/${customerId}/sites?size=100`).then(p => setSites(p.content)).catch(setErr);
  }, [customerId]);

  const submit = async (e: FormEvent) => {
    e.preventDefault(); setBusy(true); setErr(null);
    try {
      const wo = await api<WorkOrder>('/api/work-orders', 'POST', { title, description, priority, category, siteId: Number(siteId), customerId: customerId ? Number(customerId) : undefined });
      nav(`/work-orders/${wo.id}`);
    } catch (x) { setErr(x); } finally { setBusy(false); }
  };
  const prios = isCustomer ? PRIORITIES.filter(p => p !== 'URGENT') : PRIORITIES;
  return (
    <>
      <div className="page-h"><h1>{isCustomer ? 'Raise a request' : 'New work order'}</h1></div>
      <form className="card" style={{ maxWidth: 680 }} onSubmit={submit}>
        {!isCustomer && <div className="field"><label htmlFor="c">Customer</label><select id="c" required value={customerId} onChange={e => setCustomerId(e.target.value)}><option value="">Select…</option>{customers.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></div>}
        <div className="field"><label htmlFor="s">Site</label><select id="s" required value={siteId} onChange={e => setSiteId(e.target.value)}><option value="">{customerId ? 'Select a site…' : 'Choose customer first'}</option>{sites.map(s => <option key={s.id} value={s.id}>{s.name} — {s.city}</option>)}</select></div>
        <div className="field"><label htmlFor="t">Title</label><input id="t" required maxLength={150} value={title} onChange={e => setTitle(e.target.value)} placeholder="e.g. Rooftop unit not cooling" /></div>
        <div className="field"><label htmlFor="d">Description</label><textarea id="d" required maxLength={3000} value={description} onChange={e => setDescription(e.target.value)} placeholder="What is wrong, where exactly, and how urgent is it?" /></div>
        <div className="field"><label htmlFor="cat">Type of work</label><select id="cat" value={category} onChange={e => setCategory(e.target.value as Category)}>{CATEGORIES.map(c => <option key={c} value={c}>{label(c)}</option>)}</select></div>
        <div className="field"><label htmlFor="p">Priority</label><select id="p" value={priority} onChange={e => setPriority(e.target.value as Priority)}>{prios.map(p => <option key={p}>{p}</option>)}</select>
          <span className="muted small">{isCustomer ? 'For emergencies please phone dispatch.' : 'Priority sets the SLA: Urgent 4h · High 8h · Medium 24h · Low 72h.'}</span></div>
        <ErrorBox err={err} />
        <div className="row"><button className="btn" disabled={busy}>{busy ? 'Submitting…' : isCustomer ? 'Submit request' : 'Create work order'}</button><button type="button" className="btn ghost" onClick={() => nav(-1)}>Cancel</button></div>
      </form>
    </>
  );
}
