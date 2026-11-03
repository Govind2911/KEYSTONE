import { useState, type FormEvent } from 'react';
import { api, qs } from '../api';
import type { Page, Part } from '../types';
import { Empty, ErrorBox, Modal, Pager, Spinner, fmtMoney, useDebounced, useLoad } from '../ui';

function PartModal({ initial, onClose, onSaved }: { initial?: Part; onClose: () => void; onSaved: () => void }) {
  const [f, setF] = useState({ sku: initial?.sku ?? '', name: initial?.name ?? '', description: initial?.description ?? '', unitCost: String(initial?.unitCost ?? ''), stockQty: String(initial?.stockQty ?? '0') });
  const [err, setErr] = useState<unknown>(null);
  const set = (k: string) => (e: { target: { value: string } }) => setF({ ...f, [k]: e.target.value });
  const save = async (e: FormEvent) => { e.preventDefault(); try { await api(initial ? `/api/parts/${initial.id}` : '/api/parts', initial ? 'PUT' : 'POST', { ...f, unitCost: Number(f.unitCost), stockQty: Number(f.stockQty) }); onSaved(); } catch (x) { setErr(x); } };
  return (
    <Modal title={initial ? 'Edit part' : 'New part'} onClose={onClose}><form onSubmit={save}>
      <div className="grid g2"><div className="field"><label>SKU</label><input required value={f.sku} onChange={set('sku')} /></div><div className="field"><label>Name</label><input required value={f.name} onChange={set('name')} /></div>
        <div className="field"><label>Unit cost (USD)</label><input type="number" min={0} step="0.01" required value={f.unitCost} onChange={set('unitCost')} /></div>
        <div className="field"><label>Stock quantity</label><input type="number" min={0} required value={f.stockQty} onChange={set('stockQty')} /></div></div>
      <div className="field"><label>Description</label><input value={f.description} onChange={set('description')} /></div>
      <ErrorBox err={err} /><button className="btn">Save</button></form></Modal>
  );
}

export default function Parts() {
  const [q, setQ] = useState(''); const [page, setPage] = useState(0); const dq = useDebounced(q);
  const [modal, setModal] = useState<Part | 'new' | null>(null); const [restock, setRestock] = useState<Part | null>(null);
  const [qty, setQty] = useState('10'); const [err, setErr] = useState<unknown>(null);
  const { data, error, loading, reload } = useLoad(() => api<Page<Part>>('/api/parts' + qs({ q: dq, page, size: 15 })), [dq, page]);
  const doRestock = async (e: FormEvent) => { e.preventDefault(); try { await api(`/api/parts/${restock!.id}/restock`, 'POST', { quantity: Number(qty) }); setRestock(null); reload(); } catch (x) { setErr(x); } };
  return (
    <>
      <div className="page-h"><h1>Parts inventory</h1><button className="btn" onClick={() => setModal('new')}>+ New part</button></div>
      <div className="filters"><input placeholder="Search SKU or name…" value={q} onChange={e => { setQ(e.target.value); setPage(0); }} /></div>
      <ErrorBox err={error} />{loading && !data && <Spinner />}
      {data && (data.content.length === 0 ? <div className="card"><Empty title="No parts found" /></div> : (
        <div className="card" style={{ overflowX: 'auto' }}><table><thead><tr><th>SKU</th><th>Part</th><th className="n">Unit cost</th><th className="n">In stock</th><th /></tr></thead><tbody>
          {data.content.map(p => (<tr key={p.id}><td>{p.sku}</td><td>{p.name}</td><td className="n">{fmtMoney(p.unitCost)}</td>
            <td className="n" style={{ color: p.stockQty <= 3 ? 'var(--bad)' : undefined, fontWeight: p.stockQty <= 3 ? 700 : 400 }}>{p.stockQty}{p.stockQty <= 3 ? ' ⚠' : ''}</td>
            <td className="n"><button className="btn ghost sm" onClick={() => { setRestock(p); setQty('10'); setErr(null); }}>Restock</button> <button className="btn ghost sm" onClick={() => setModal(p)}>Edit</button></td></tr>))}</tbody></table></div>))}
      {data && <Pager page={data} onPage={setPage} />}
      {modal && <PartModal initial={modal === 'new' ? undefined : modal} onClose={() => setModal(null)} onSaved={() => { setModal(null); reload(); }} />}
      {restock && <Modal title={`Restock ${restock.name}`} onClose={() => setRestock(null)}><form onSubmit={doRestock}><div className="field"><label>Quantity to add</label><input type="number" min={1} required value={qty} onChange={e => setQty(e.target.value)} /></div><ErrorBox err={err} /><button className="btn">Add stock</button></form></Modal>}
    </>
  );
}
