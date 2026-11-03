import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { api, getToken, setToken } from './api';
import type { LoginResponse, User } from './types';

interface Ctx { user: User | null; ready: boolean; login: (email: string, password: string) => Promise<User>; logout: () => void }
const AuthCtx = createContext<Ctx>(null as unknown as Ctx);
export const useAuth = () => useContext(AuthCtx);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const onLogout = () => setUser(null);
    window.addEventListener('ks-logout', onLogout);
    if (getToken()) api<User>('/api/auth/me').then(setUser).catch(() => setToken(null)).finally(() => setReady(true));
    else setReady(true);
    return () => window.removeEventListener('ks-logout', onLogout);
  }, []);

  const login = async (email: string, password: string) => {
    const r = await api<LoginResponse>('/api/auth/login', 'POST', { email, password });
    setToken(r.token); setUser(r.user); return r.user;
  };
  const logout = () => { setToken(null); setUser(null); };
  return <AuthCtx.Provider value={{ user, ready, login, logout }}>{children}</AuthCtx.Provider>;
}

export const isStaff = (u: User) => u.role !== 'CUSTOMER';
export const isDispatchOrManager = (u: User) => u.role === 'DISPATCHER' || u.role === 'MANAGER';
export const homeFor = (u: User) =>
  u.role === 'MANAGER' ? '/dashboard' : u.role === 'DISPATCHER' ? '/board' : '/work-orders';
