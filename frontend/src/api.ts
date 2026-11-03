const BASE: string = (import.meta.env.VITE_API_URL as string | undefined) ?? '';

export class ApiError extends Error {
  status: number;
  fields: { field: string; message: string }[];
  constructor(status: number, message: string, fields: { field: string; message: string }[] = []) {
    super(message); this.status = status; this.fields = fields;
  }
  /** Human-readable text including per-field problems. */
  get full(): string {
    return this.fields.length ? `${this.message}: ${this.fields.map(f => `${f.field} ${f.message}`).join('; ')}` : this.message;
  }
}

let token: string | null = localStorage.getItem('ks_token');
export const getToken = () => token;
export function setToken(t: string | null) {
  token = t;
  if (t) localStorage.setItem('ks_token', t); else localStorage.removeItem('ks_token');
}

async function request(path: string, init: RequestInit): Promise<Response> {
  const headers = new Headers(init.headers);
  if (token) headers.set('Authorization', `Bearer ${token}`);
  let res: Response;
  try { res = await fetch(BASE + path, { ...init, headers }); }
  catch { throw new ApiError(0, 'Cannot reach the server. Check your connection.'); }
  if (!res.ok) {
    let msg = res.statusText, fields: { field: string; message: string }[] = [];
    try { const j = await res.json(); msg = j.message ?? msg; fields = j.errors ?? []; } catch { /* no body */ }
    if (res.status === 401 && token) { setToken(null); window.dispatchEvent(new Event('ks-logout')); }
    throw new ApiError(res.status, msg, fields);
  }
  return res;
}

export async function api<T>(path: string, method = 'GET', body?: unknown): Promise<T> {
  const res = await request(path, {
    method,
    headers: body !== undefined ? { 'Content-Type': 'application/json' } : undefined,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  return (res.status === 204 ? undefined : await res.json()) as T;
}

export async function upload<T>(path: string, file: File): Promise<T> {
  const form = new FormData(); form.append('file', file);
  return (await (await request(path, { method: 'POST', body: form })).json()) as T;
}

export async function download(path: string, fileName: string) {
  const blob = await (await request(path, { method: 'GET' })).blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a'); a.href = url; a.download = fileName; a.click();
  URL.revokeObjectURL(url);
}

export const qs = (o: Record<string, string | number | boolean | undefined | null | string[]>) => {
  const p = new URLSearchParams();
  Object.entries(o).forEach(([k, v]) => {
    if (v === undefined || v === null || v === '' || v === false) return;
    p.set(k, Array.isArray(v) ? v.join(',') : String(v));
  });
  const s = p.toString(); return s ? `?${s}` : '';
};

/** Fetches a protected file (needs the bearer token) as a Blob, e.g. for image previews. */
export async function fetchBlob(path: string): Promise<Blob> {
  return (await request(path, { method: 'GET' })).blob();
}
