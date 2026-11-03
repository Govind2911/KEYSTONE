import { useState, type FormEvent } from 'react';
import { api, qs } from '../api';
import { useAuth } from '../auth';
import type { Customer, Page, Role, User } from '../types';
import { Empty, ErrorBox, Modal, Pager, Spinner, label, useLoad } from '../ui';

const ROLES: Role[] = ['MANAGER', 'DISPATCHER', 'TECHNICIAN', 'CUSTOMER'];

function UserModal({ initial, customers, onClose, onSaved }: { initial?: User; customers: Customer[]; onClose: () => void; onSaved: () => void }) {
  const [f, setF] = useState({ name: initial?.name ?? '', email: initial?.email ?? '', password: '', phone: initial?.phone ?? '', role: (initial?.role ?? 'TECHNICIAN') as Role, customerId: String(initial?.customerId ?? ''), active: initial?.active ?? true, skills: initial?.skills ?? '' });
  const [err, setErr] = useState<unknown>(null);
  const set = (k: string) => (e: { target: { value: string } }) => setF({ ...f, [k]: e.target.value });
  const save = async (e: FormEvent) => {
    e.preventDefault();
    const body = { name: f.name, phone: f.phone, role: f.role, active: f.active, skills: f.role === 'TECHNICIAN' ? f.skills : null, customerId: f.role === 'CUSTOMER' ? Number(f.customerId) : null, ...(initial ? {} : { email: f.email, password: f.password }) };
    try { await api(initial ? `/api/users/${initial.id}` : '/api/users', initial ? 'PUT' : 'POST', body); onSaved(); } catch (x) { setErr(x); }
  };
  return (
    <Modal title={initial ? `Edit ${initial.name}` : 'New user'} onClose={onClose}><form onSubmit={save}>
      <div className="field"><label>Name</label><input required value={f.name} onChange={set('name')} /></div>
      {!initial && <><div className="field"><label>Email</label><input type="email" required value={f.email} onChange={set('email')} /></div>
        <div className="field"><label>Initial password (min 8 characters)</label><input type="password" minLength={8} required value={f.password} onChange={set('password')} /></div></>}
      <div className="grid g2"><div className="field"><label>Role</label><select value={f.role} onChange={set('role')}>{ROLES.map(r => <option key={r} value={r}>{label(r)}</option>)}</select></div>
        <div className="field"><label>Phone</label><input value={f.phone} onChange={set('phone')} /></div></div>
      {f.role === 'TECHNICIAN' && <div className="field"><label>Skills (used to suggest technicians when dispatching)</label><div className="row">{(['HVAC', 'ELECTRICAL', 'PLUMBING'] as const).map(s => (
        <label key={s} style={{ display: 'flex', gap: 6, alignItems: 'center', margin: 0 }}><input type="checkbox" style={{ width: 'auto' }} checked={f.skills.split(',').includes(s)}
          onChange={e => { const cur = f.skills.split(',').filter(Boolean); setF({ ...f, skills: (e.target.checked ? [...cur, s] : cur.filter(x => x !== s)).join(',') }); }} />{label(s)}</label>))}</div></div>}
      {f.role === 'CUSTOMER' && <div className="field"><label>Customer organisation</label><select required value={f.customerId} onChange={set('customerId')}><option value="">Select…</option>{customers.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></div>}
      {initial && <div className="field"><label><input type="checkbox" style={{ width: 'auto' }} checked={f.active} onChange={e => setF({ ...f, active: e.target.checked })} /> Active (can sign in)</label></div>}
      <ErrorBox err={err} /><button className="btn">Save</button></form></Modal>
  );
}

export default function Users() {
  const { user: me } = useAuth();
  const [role, setRole] = useState(''); const [page, setPage] = useState(0);
  const [modal, setModal] = useState<User | 'new' | null>(null); const [reset, setReset] = useState<User | null>(null);
  const [pw, setPw] = useState(''); const [err, setErr] = useState<unknown>(null); const [ok, setOk] = useState('');
  const customers = useLoad(() => api<Page<Customer>>('/api/customers?size=100'), []);
  const { data, error, loading, reload } = useLoad(() => api<Page<User>>('/api/users' + qs({ role, page, size: 15 })), [role, page]);
  const doReset = async (e: FormEvent) => { e.preventDefault(); try { await api(`/api/users/${reset!.id}/password`, 'POST', { newPassword: pw }); setOk(`Password reset for ${reset!.name}.`); setReset(null); setPw(''); } catch (x) { setErr(x); } };
  return (
    <>
      <div className="page-h"><h1>Users</h1><button className="btn" onClick={() => setModal('new')}>+ New user</button></div>
      <div className="filters"><select value={role} onChange={e => { setRole(e.target.value); setPage(0); }} aria-label="Role"><option value="">All roles</option>{ROLES.map(r => <option key={r} value={r}>{label(r)}</option>)}</select></div>
      {ok && <div className="alert ok">{ok}</div>}<ErrorBox err={error} />{loading && !data && <Spinner />}
      {data && (data.content.length === 0 ? <div className="card"><Empty title="No users" /></div> : (
        <div className="card" style={{ overflowX: 'auto' }}><table><thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Organisation / skills</th><th>Status</th><th /></tr></thead><tbody>
          {data.content.map(u => (<tr key={u.id}><td>{u.name}</td><td>{u.email}</td><td>{label(u.role)}</td><td>{u.customerName ?? (u.skills ? u.skills.split(',').join(', ') : '—')}</td><td>{u.active ? 'Active' : <span className="pill st-CANCELLED">Inactive</span>}</td>
            <td className="n"><button className="btn ghost sm" onClick={() => setModal(u)}>Edit</button> <button className="btn ghost sm" onClick={() => { setReset(u); setErr(null); setOk(''); }}>Reset password</button>{u.id === me?.id ? '' : ''}</td></tr>))}</tbody></table></div>))}
      {data && <Pager page={data} onPage={setPage} />}
      {modal && <UserModal initial={modal === 'new' ? undefined : modal} customers={customers.data?.content ?? []} onClose={() => setModal(null)} onSaved={() => { setModal(null); reload(); }} />}
      {reset && <Modal title={`Reset password · ${reset.name}`} onClose={() => setReset(null)}><form onSubmit={doReset}><div className="field"><label>New password (min 8 characters)</label><input type="password" minLength={8} required value={pw} onChange={e => setPw(e.target.value)} /></div><ErrorBox err={err} /><button className="btn">Reset password</button></form></Modal>}
    </>
  );
}
