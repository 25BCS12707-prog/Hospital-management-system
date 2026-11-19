import { useCallback, useEffect, useState, createContext, useContext } from 'react';

export const money = (n) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(n || 0);

export const todayStr = () => {
  const d = new Date();
  const p = (x) => String(x).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
};

export const prettyDate = (s) =>
  new Date(s + 'T00:00:00').toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });

const cap = (s) => (s ? s[0] + s.slice(1).toLowerCase() : '');

/** Loads data from the backend and lets a page reload it. */
export function useLoad(fn, deps = []) {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const run = useCallback(() => {
    return fn().then((d) => { setData(d); setError(''); }).catch((e) => setError(e.message));
  }, deps); // eslint-disable-line
  useEffect(() => { run(); }, [run]);
  return { data, error, loading: data === null && !error, reload: run };
}

const ToastCtx = createContext(() => {});
export const useToast = () => useContext(ToastCtx);

export function ToastProvider({ children }) {
  const [t, setT] = useState(null);
  const show = useCallback((msg, type = 'ok') => {
    setT({ msg, type });
    setTimeout(() => setT(null), 3500);
  }, []);
  return (
    <ToastCtx.Provider value={show}>
      {children}
      {t && <div className={'toast ' + t.type}>{t.msg}</div>}
    </ToastCtx.Provider>
  );
}

export function Badge({ status }) {
  return <span className={'badge ' + status}>{cap(status)}</span>;
}

export function Modal({ title, onClose, children }) {
  useEffect(() => {
    const h = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', h);
    return () => window.removeEventListener('keydown', h);
  }, [onClose]);
  return (
    <div className="overlay" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-modal="true">
        <div className="modal-head"><h3>{title}</h3><button className="x" onClick={onClose} aria-label="Close">×</button></div>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  );
}

export function Field({ label, children }) {
  return <div className="field"><label>{label}</label>{children}</div>;
}

export function Card({ title, actions, children, flush }) {
  return (
    <div className="card">
      {(title || actions) && <div className="card-head"><h3>{title}</h3>{actions}</div>}
      <div className={flush ? '' : 'card-body'} style={flush ? { marginTop: 14 } : undefined}>{children}</div>
    </div>
  );
}

export function Stat({ icon, value, label }) {
  return (
    <div className="card stat">
      <div className="icon">{icon}</div>
      <div><div className="num">{value}</div><div className="lbl">{label}</div></div>
    </div>
  );
}

export function Table({ cols, rows, empty = 'Nothing to show yet.' }) {
  if (!rows.length) return <div className="empty">{empty}</div>;
  return (
    <div className="table-wrap">
      <table>
        <thead><tr>{cols.map((c) => <th key={c.label}>{c.label}</th>)}</tr></thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={r.id || i}>{cols.map((c) => <td key={c.label}>{c.render ? c.render(r) : r[c.key]}</td>)}</tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function State({ loading, error, children }) {
  if (error) return <div className="alert">{error}</div>;
  if (loading) return <div className="loading">Loading…</div>;
  return children;
}

/** Submit helper: runs an async action, shows toast, reports error inside a form. */
export function useAction() {
  const toast = useToast();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const run = async (fn, okMsg) => {
    setBusy(true); setError('');
    try { const r = await fn(); if (okMsg) toast(okMsg); return r; }
    catch (e) { setError(e.message); toast(e.message, 'err'); return undefined; }
    finally { setBusy(false); }
  };
  return { run, busy, error, setError };
}
