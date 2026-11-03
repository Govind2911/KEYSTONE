import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { homeFor, useAuth } from '../auth';
import { ErrorBox } from '../ui';

const DEMO = [['manager@meridian.test', 'Manager'], ['dispatcher@meridian.test', 'Dispatcher'], ['tech1@meridian.test', 'Technician'], ['customer1@northgate.test', 'Customer']];

export default function Login() {
  const { login } = useAuth(); const nav = useNavigate();
  const [email, setEmail] = useState(''); const [password, setPassword] = useState('');
  const [err, setErr] = useState<unknown>(null); const [busy, setBusy] = useState(false);
  const submit = async (e: FormEvent) => {
    e.preventDefault(); setBusy(true); setErr(null);
    try { nav(homeFor(await login(email, password)), { replace: true }); } catch (x) { setErr(x); } finally { setBusy(false); }
  };
  return (
    <div className="login"><form className="card" onSubmit={submit}>
      <div className="brand" style={{ color: 'var(--ink)', paddingLeft: 0 }}><i />KEYSTONE</div>
      <p className="muted" style={{ marginTop: 0 }}>Field service management for Meridian Facilities</p>
      <div className="field"><label htmlFor="e">Email</label><input id="e" type="email" required autoFocus value={email} onChange={e => setEmail(e.target.value)} /></div>
      <div className="field"><label htmlFor="p">Password</label><input id="p" type="password" required value={password} onChange={e => setPassword(e.target.value)} /></div>
      <ErrorBox err={err} />
      <button className="btn" style={{ width: '100%' }} disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
      <div className="demo">Demo logins (password <b>Password@123</b>) — click to fill:<br />
        {DEMO.map(([m, r]) => <span key={m}><code onClick={() => { setEmail(m); setPassword('Password@123'); }}>{r}</code>{' · '}</span>)}
      </div>
    </form></div>
  );
}
