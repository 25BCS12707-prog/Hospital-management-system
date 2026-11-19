import { useState } from 'react';
import { api, session } from './api.js';
import { ToastProvider, Field, useAction } from './ui.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Patients from './pages/Patients.jsx';
import Doctors from './pages/Doctors.jsx';
import Appointments from './pages/Appointments.jsx';
import Billing from './pages/Billing.jsx';
import Records from './pages/Records.jsx';

const NAV = {
  admin: [['dashboard', '🏠', 'Dashboard'], ['patients', '🧑‍🤝‍🧑', 'Patients'], ['doctors', '🩺', 'Doctors'],
    ['appointments', '📅', 'Appointments'], ['billing', '💳', 'Billing']],
  doctor: [['dashboard', '🏠', 'Dashboard'], ['appointments', '📅', 'Appointments'], ['patients', '🧑‍🤝‍🧑', 'My Patients'],
    ['records', '📋', 'Treatments']],
  patient: [['dashboard', '🏠', 'Dashboard'], ['doctors', '🩺', 'Find a Doctor'], ['appointments', '📅', 'My Appointments'],
    ['billing', '💳', 'My Bills'], ['records', '📋', 'Medical Records']],
};

const Logo = () => (
  <div className="logo">
    <div className="logo-mark">
      <svg width="20" height="20" viewBox="0 0 32 32"><path d="M13 5h6v8h8v6h-8v8h-6v-8H5v-6h8z" fill="#fff" /></svg>
    </div>
    MediCare
  </div>
);

function Login({ onLogin }) {
  const [role, setRole] = useState('patient');
  const [mode, setMode] = useState('login'); // login | register
  const [f, setF] = useState({});
  const { run, busy, error, setError } = useAction();
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const switchRole = (r) => { setRole(r); setMode('login'); setF({}); setError(''); };

  const submit = async (e) => {
    e.preventDefault();
    if (mode === 'register') {
      const body = role === 'patient'
        ? { name: f.name, age: f.age, gender: f.gender || 'MALE', phone: f.phone, email: f.email }
        : { name: f.name, specialization: f.specialization, fee: f.fee, phone: f.phone, email: f.email };
      const u = await run(() => api.register(role, body));
      if (u) { setMode('login'); setF({ key: u.id }); alert(`Registration successful. Your login ID is ${u.id}`); }
      return;
    }
    const body = role === 'admin' ? { email: f.email, password: f.password } : { key: f.key };
    const r = await run(() => api.login(role, body));
    if (r) onLogin(r);
  };

  return (
    <div className="login-wrap">
      <div className="login-hero">
        <Logo />
        <h1>Modern care starts with well-run operations.</h1>
        <p>One secure place for patients, doctors, appointments and billing — powered by your Java hospital system.</p>
        <ul><li>Smart appointment scheduling with no double booking</li><li>Diagnosis, prescription and automatic billing</li><li>Separate dashboards for Admin, Doctor and Patient</li></ul>
      </div>
      <div className="login-panel">
        <form className="login-card" onSubmit={submit}>
          <h2>{mode === 'login' ? 'Welcome back' : `Register as ${role}`}</h2>
          <div className="sub">{mode === 'login' ? 'Sign in to continue' : 'Create your account'}</div>
          <div className="tabs">
            {['patient', 'doctor', 'admin'].map((r) => (
              <button type="button" key={r} className={role === r ? 'active' : ''} onClick={() => switchRole(r)}>
                {r[0].toUpperCase() + r.slice(1)}
              </button>
            ))}
          </div>
          {error && <div className="alert">{error}</div>}

          {mode === 'login' && role === 'admin' && (<>
            <Field label="Email"><input type="email" required value={f.email || ''} onChange={set('email')} placeholder="admin@hospital.com" /></Field>
            <Field label="Password"><input type="password" required value={f.password || ''} onChange={set('password')} /></Field>
          </>)}
          {mode === 'login' && role !== 'admin' && (
            <Field label={role === 'doctor' ? 'Doctor ID or email' : 'Patient ID or email'}>
              <input required value={f.key || ''} onChange={set('key')} placeholder={role === 'doctor' ? 'e.g. D001' : 'e.g. P001'} />
            </Field>
          )}

          {mode === 'register' && (<>
            <Field label="Full name"><input required value={f.name || ''} onChange={set('name')} /></Field>
            <div className="row">
              <Field label="Phone (10 digits)"><input required value={f.phone || ''} onChange={set('phone')} inputMode="numeric" maxLength={10} /></Field>
              <Field label="Email"><input type="email" required value={f.email || ''} onChange={set('email')} /></Field>
            </div>
            {role === 'patient' ? (
              <div className="row">
                <Field label="Age"><input type="number" min="0" max="120" required value={f.age || ''} onChange={set('age')} /></Field>
                <Field label="Gender">
                  <select value={f.gender || 'MALE'} onChange={set('gender')}><option value="MALE">Male</option><option value="FEMALE">Female</option><option value="OTHER">Other</option></select>
                </Field>
              </div>
            ) : (
              <div className="row">
                <Field label="Specialization"><input required value={f.specialization || ''} onChange={set('specialization')} /></Field>
                <Field label="Consultation fee (₹)"><input type="number" min="1" required value={f.fee || ''} onChange={set('fee')} /></Field>
              </div>
            )}
          </>)}

          <button className="btn" style={{ width: '100%', justifyContent: 'center' }} disabled={busy}>
            {busy ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}
          </button>
          {role !== 'admin' && (
            <div className="center">
              {mode === 'login' ? <>New here? <button type="button" className="link" onClick={() => { setMode('register'); setError(''); setF({}); }}>Register as {role}</button></>
                : <>Already registered? <button type="button" className="link" onClick={() => { setMode('login'); setError(''); setF({}); }}>Sign in</button></>}
            </div>
          )}
        </form>
      </div>
    </div>
  );
}

function Shell({ auth, onLogout }) {
  const nav = NAV[auth.role];
  const [page, setPage] = useState('dashboard');
  const [open, setOpen] = useState(false);
  const go = (p) => { setPage(p); setOpen(false); };
  const title = nav.find((n) => n[0] === page)?.[2] || 'Dashboard';
  const props = { role: auth.role, user: auth.user, go };
  const view = {
    dashboard: <Dashboard {...props} />, patients: <Patients {...props} />, doctors: <Doctors {...props} />,
    appointments: <Appointments {...props} />, billing: <Billing {...props} />, records: <Records {...props} />,
  }[page];

  return (
    <div className="shell">
      <aside className={'sidebar' + (open ? ' open' : '')}>
        <Logo />
        {nav.map(([id, icon, label]) => (
          <button key={id} className={'nav-item' + (page === id ? ' active' : '')} onClick={() => go(id)}><span>{icon}</span>{label}</button>
        ))}
        <div className="spacer" />
        <button className="nav-item" onClick={onLogout}><span>🚪</span>Sign out</button>
      </aside>
      <div className="main">
        <header className="topbar">
          <div className="toolbar">
            <button className="btn secondary sm menu-btn" onClick={() => setOpen(!open)} aria-label="Menu">☰</button>
            <h1>{title}</h1>
          </div>
          <div className="user-chip">
            <div className="who" style={{ textAlign: 'right' }}>
              <strong>{auth.user.name}</strong><small>{auth.user.role} · {auth.user.id}</small>
            </div>
            <div className="avatar">{auth.user.name.split(' ').map((w) => w[0]).slice(0, 2).join('')}</div>
          </div>
        </header>
        <main className="content" key={page}>{view}</main>
      </div>
    </div>
  );
}

export default function App() {
  const [auth, setAuth] = useState(() => session.get());
  const login = (r) => { session.set(r); setAuth(r); };
  const logout = async () => { try { await api.logout(); } catch { /* ignore */ } session.clear(); setAuth(null); };
  return <ToastProvider>{auth ? <Shell auth={auth} onLogout={logout} /> : <Login onLogin={login} />}</ToastProvider>;
}
