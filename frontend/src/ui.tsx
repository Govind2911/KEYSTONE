import { useEffect, useState, type ReactNode } from 'react';
import type { Category, Page, Priority, Sla, Status, Technician } from './types';
import { ApiError, fetchBlob } from './api';

export const fmtDate = (s?: string) => s ? new Date(s).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' }) : '-';
export const fmtMoney = (n?: number) => n === undefined ? '-' : n.toLocaleString([], { style: 'currency', currency: 'USD' });
export const fmtMinutes = (m?: number) => m === undefined ? '-' : `${Math.floor(m / 60)}h ${m % 60}m`;
export const label = (s: string) => s.replace('_', ' ').toLowerCase().replace(/^\w/, c => c.toUpperCase());
export function timeLeft(due?: string): string {
  if (!due) return '';
  const ms = new Date(due).getTime() - Date.now(); const abs = Math.abs(ms);
  const h = Math.floor(abs / 3600000), m = Math.floor((abs % 3600000) / 60000);
  const t = h >= 48 ? `${Math.floor(h / 24)}d` : h > 0 ? `${h}h ${m}m` : `${m}m`;
  return ms < 0 ? `${t} overdue` : `${t} left`;
}

export const StatusPill = ({ s }: { s: Status }) => <span className={`pill st-${s}`}>{label(s)}</span>;
export const PriorityTag = ({ p }: { p: Priority }) => <span className={`prio pr-${p}`}>{p}</span>;

/** The SLA "fuel gauge": fills as the response window is used up. */
export function SlaBar({ status, pct, due }: { status?: Sla; pct?: number; due?: string }) {
  if (!status || status === 'NONE') return null;
  const cls = status === 'BREACHED' ? 'bad' : status === 'AT_RISK' ? 'warn' : status === 'MET' ? 'met' : 'ok';
  const w = status === 'MET' ? 100 : Math.max(4, pct ?? 0);
  return (
    <div className={`sla ${cls}`} title={`SLA due ${fmtDate(due)}`}>
      <div className="sla-track"><div className="sla-fill" style={{ width: `${w}%` }} /></div>
      <span>{status === 'MET' ? 'SLA met' : status === 'BREACHED' ? `Breached${due ? ' · ' + timeLeft(due) : ''}` : timeLeft(due)}</span>
    </div>
  );
}

export function Spinner() { return <div className="center muted"><span className="spin" /> Loading…</div>; }
export function Empty({ title, hint, action }: { title: string; hint?: string; action?: ReactNode }) {
  return <div className="empty"><strong>{title}</strong>{hint && <p>{hint}</p>}{action}</div>;
}
export function ErrorBox({ err }: { err: unknown }) {
  if (!err) return null;
  const msg = err instanceof ApiError ? err.full : (err as Error).message;
  return <div className="alert err" role="alert">{msg}</div>;
}

export function Pager<T>({ page, onPage }: { page: Page<T>; onPage: (n: number) => void }) {
  if (page.totalPages <= 1) return <div className="muted small pager">{page.totalElements} result{page.totalElements === 1 ? '' : 's'}</div>;
  return (
    <div className="pager">
      <button className="btn ghost" disabled={page.page === 0} onClick={() => onPage(page.page - 1)}>← Prev</button>
      <span className="muted small">Page {page.page + 1} of {page.totalPages} · {page.totalElements} results</span>
      <button className="btn ghost" disabled={page.page + 1 >= page.totalPages} onClick={() => onPage(page.page + 1)}>Next →</button>
    </div>
  );
}

export function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  useEffect(() => { const h = (e: KeyboardEvent) => e.key === 'Escape' && onClose(); window.addEventListener('keydown', h); return () => window.removeEventListener('keydown', h); }, [onClose]);
  return (
    <div className="modal-bg" onClick={onClose}>
      <div className="modal" role="dialog" aria-label={title} onClick={e => e.stopPropagation()}>
        <div className="modal-h"><h3>{title}</h3><button className="btn ghost" onClick={onClose} aria-label="Close">✕</button></div>
        {children}
      </div>
    </div>
  );
}

/** Small data-loading hook with loading / error / reload. */
export function useLoad<T>(fn: () => Promise<T>, deps: unknown[]) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(true);
  const [tick, setTick] = useState(0);
  useEffect(() => {
    let live = true; setLoading(true);
    fn().then(d => { if (live) { setData(d); setError(null); } }).catch(e => live && setError(e)).finally(() => live && setLoading(false));
    return () => { live = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick]);
  return { data, error, loading, reload: () => setTick(t => t + 1), setData };
}

export function useDebounced<T>(v: T, ms = 300): T {
  const [d, setD] = useState(v);
  useEffect(() => { const t = setTimeout(() => setD(v), ms); return () => clearTimeout(t); }, [v, ms]);
  return d;
}

export const fmtSize = (b: number) => (b < 1024 * 1024 ? `${Math.max(1, Math.round(b / 1024))} KB` : `${(b / 1024 / 1024).toFixed(1)} MB`);

/** Skill-matching technicians first, then least loaded. */
export function sortTechs(techs: Technician[], category?: Category): Technician[] {
  return techs
    .map(t => ({ ...t, skillMatch: !!category && !!t.skills && t.skills.split(',').includes(category) }))
    .sort((a, b) => Number(b.skillMatch) - Number(a.skillMatch) || a.openJobs - b.openJobs || a.name.localeCompare(b.name));
}

/** Image from a protected endpoint: fetched with the token, shown via an object URL. */
export function AuthImage({ path, alt, onClick }: { path: string; alt: string; onClick?: () => void }) {
  const [src, setSrc] = useState<string | null>(null);
  useEffect(() => {
    let live = true; let url: string | null = null;
    fetchBlob(path).then(b => { if (live) { url = URL.createObjectURL(b); setSrc(url); } }).catch(() => {});
    return () => { live = false; if (url) URL.revokeObjectURL(url); };
  }, [path]);
  return src ? <img className="thumb" src={src} alt={alt} onClick={onClick} /> : <div className="thumb doc">…</div>;
}
