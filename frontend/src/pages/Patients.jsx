import { useState } from 'react';
import { api } from '../api.js';
import { Card, Field, Modal, State, Table, prettyDate, useAction, useLoad } from '../ui.jsx';

function AddPatient({ onClose, onDone }) {
  const [f, setF] = useState({ gender: 'MALE' });
  const { run, busy, error } = useAction();
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const submit = async (e) => {
    e.preventDefault();
    const p = await run(() => api.register('patient', f));
    if (p) { onDone(`Patient ${p.name} registered (${p.id}).`); }
  };
  return (
    <Modal title="Register new patient" onClose={onClose}>
      <form onSubmit={submit}>
        {error && <div className="alert">{error}</div>}
        <Field label="Full name"><input required value={f.name || ''} onChange={set('name')} /></Field>
        <div className="row">
          <Field label="Age"><input type="number" min="0" max="120" required value={f.age || ''} onChange={set('age')} /></Field>
          <Field label="Gender"><select value={f.gender} onChange={set('gender')}><option value="MALE">Male</option><option value="FEMALE">Female</option><option value="OTHER">Other</option></select></Field>
        </div>
        <div className="row">
          <Field label="Phone (10 digits)"><input required maxLength={10} value={f.phone || ''} onChange={set('phone')} /></Field>
          <Field label="Email"><input type="email" required value={f.email || ''} onChange={set('email')} /></Field>
        </div>
        <button className="btn" disabled={busy}>{busy ? 'Saving…' : 'Register patient'}</button>
      </form>
    </Modal>
  );
}

function Details({ id, onClose }) {
  const d = useLoad(() => api.patient(id), [id]);
  const p = d.data;
  return (
    <Modal title="Patient record" onClose={onClose}>
      <State loading={d.loading} error={d.error}>
        {p && (<>
          <h3>{p.name} <span className="sub-text">({p.id})</span></h3>
          <p className="sub-text">{p.age} yrs · {p.gender} · {p.phone} · {p.email}</p>
          <h4>Treatment history</h4>
          {p.treatments.length === 0 && <div className="sub-text">No treatments recorded yet.</div>}
          {p.treatments.map((t) => (
            <div className="record" key={t.id}>
              <strong>{prettyDate(t.date)}</strong> · Dr. {t.doctorName}
              <div><span className="sub-text">Diagnosis:</span> {t.diagnosis}</div>
              <div><span className="sub-text">Prescription:</span> {t.prescription}</div>
            </div>
          ))}
        </>)}
      </State>
    </Modal>
  );
}

export default function Patients({ role }) {
  const list = useLoad(api.patients);
  const [q, setQ] = useState('');
  const [adding, setAdding] = useState(false);
  const [view, setView] = useState(null);
  const [msg, setMsg] = useState('');
  const rows = (list.data || []).filter((p) => (p.name + p.id + p.email + p.phone).toLowerCase().includes(q.toLowerCase()));
  return (
    <>
      {msg && <div className="alert" style={{ background: 'var(--green-bg)', color: 'var(--green)' }}>{msg}</div>}
      <Card flush title={role === 'admin' ? 'All patients' : 'Patients with your appointments'}
        actions={<div className="toolbar">
          <input className="search" placeholder="Search patients…" value={q} onChange={(e) => setQ(e.target.value)} />
          {role === 'admin' && <button className="btn" onClick={() => setAdding(true)}>+ Add patient</button>}
        </div>}>
        <State loading={list.loading} error={list.error}>
          <Table rows={rows} empty="No patients found." cols={[
            { label: 'ID', key: 'id' }, { label: 'Name', render: (p) => <strong>{p.name}</strong> },
            { label: 'Age', key: 'age' }, { label: 'Gender', render: (p) => p.gender[0] + p.gender.slice(1).toLowerCase() },
            { label: 'Phone', key: 'phone' }, { label: 'Email', key: 'email' },
            { label: '', render: (p) => <button className="btn secondary sm" onClick={() => setView(p.id)}>View record</button> },
          ]} />
        </State>
      </Card>
      {adding && <AddPatient onClose={() => setAdding(false)} onDone={(m) => { setAdding(false); setMsg(m); list.reload(); }} />}
      {view && <Details id={view} onClose={() => setView(null)} />}
    </>
  );
}
