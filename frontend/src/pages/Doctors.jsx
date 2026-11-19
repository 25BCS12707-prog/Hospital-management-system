import { useState } from 'react';
import { api } from '../api.js';
import { Card, Field, Modal, State, Table, money, useAction, useLoad } from '../ui.jsx';
import { BookModal } from './Appointments.jsx';

function AddDoctor({ onClose, onDone }) {
  const [f, setF] = useState({});
  const { run, busy, error } = useAction();
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const submit = async (e) => {
    e.preventDefault();
    const d = await run(() => api.register('doctor', f));
    if (d) onDone(`Dr. ${d.name} registered (${d.id}).`);
  };
  return (
    <Modal title="Register new doctor" onClose={onClose}>
      <form onSubmit={submit}>
        {error && <div className="alert">{error}</div>}
        <Field label="Full name"><input required value={f.name || ''} onChange={set('name')} /></Field>
        <div className="row">
          <Field label="Specialization"><input required value={f.specialization || ''} onChange={set('specialization')} /></Field>
          <Field label="Consultation fee (₹)"><input type="number" min="1" required value={f.fee || ''} onChange={set('fee')} /></Field>
        </div>
        <div className="row">
          <Field label="Phone (10 digits)"><input required maxLength={10} value={f.phone || ''} onChange={set('phone')} /></Field>
          <Field label="Email"><input type="email" required value={f.email || ''} onChange={set('email')} /></Field>
        </div>
        <button className="btn" disabled={busy}>{busy ? 'Saving…' : 'Register doctor'}</button>
      </form>
    </Modal>
  );
}

export default function Doctors({ role, user }) {
  const list = useLoad(api.doctors);
  const [q, setQ] = useState('');
  const [adding, setAdding] = useState(false);
  const [booking, setBooking] = useState(null);
  const [msg, setMsg] = useState('');
  const rows = (list.data || []).filter((d) => (d.name + d.specialization + d.id).toLowerCase().includes(q.toLowerCase()));
  const cols = [
    { label: 'ID', key: 'id' }, { label: 'Doctor', render: (d) => <strong>Dr. {d.name}</strong> },
    { label: 'Specialization', key: 'specialization' }, { label: 'Fee', render: (d) => money(d.fee) },
    { label: 'Phone', key: 'phone' }, { label: 'Email', key: 'email' },
  ];
  if (role !== 'doctor') cols.push({ label: '', render: (d) => <button className="btn sm" onClick={() => setBooking(d.id)}>Book</button> });
  return (
    <>
      {msg && <div className="alert" style={{ background: 'var(--green-bg)', color: 'var(--green)' }}>{msg}</div>}
      <Card flush title={role === 'patient' ? 'Available doctors' : 'All doctors'}
        actions={<div className="toolbar">
          <input className="search" placeholder="Search name or specialty…" value={q} onChange={(e) => setQ(e.target.value)} />
          {role === 'admin' && <button className="btn" onClick={() => setAdding(true)}>+ Add doctor</button>}
        </div>}>
        <State loading={list.loading} error={list.error}><Table rows={rows} cols={cols} empty="No doctors found." /></State>
      </Card>
      {adding && <AddDoctor onClose={() => setAdding(false)} onDone={(m) => { setAdding(false); setMsg(m); list.reload(); }} />}
      {booking && <BookModal role={role} user={user} doctorId={booking} onClose={() => setBooking(null)}
        onDone={() => { setBooking(null); setMsg('Appointment booked — it will appear under Appointments.'); }} />}
    </>
  );
}
