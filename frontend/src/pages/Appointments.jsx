import { useEffect, useState } from 'react';
import { api } from '../api.js';
import { Badge, Card, Field, Modal, State, Table, prettyDate, todayStr, useAction, useLoad, useToast } from '../ui.jsx';

export function BookModal({ role, user, doctorId, onClose, onDone }) {
  const doctors = useLoad(api.doctors);
  const patients = useLoad(() => (role === 'admin' ? api.patients() : Promise.resolve([])), [role]);
  const [f, setF] = useState({ doctorId: doctorId || '', date: todayStr(), time: '', patientId: '' });
  const [slots, setSlots] = useState([]);
  const { run, busy, error, setError } = useAction();

  useEffect(() => {
    if (!f.doctorId || !f.date) return;
    let live = true;
    api.slots(f.doctorId, f.date).then((s) => { if (live) { setSlots(s); setError(''); } })
      .catch((e) => { if (live) { setSlots([]); setError(e.message); } });
    return () => { live = false; };
  }, [f.doctorId, f.date]); // eslint-disable-line

  const submit = async (e) => {
    e.preventDefault();
    if (!f.time) return setError('Please choose a time slot.');
    const a = await run(() => api.book({ ...f, patientId: role === 'admin' ? f.patientId : undefined }), 'Appointment booked');
    if (a) onDone();
  };

  return (
    <Modal title="Book appointment" onClose={onClose}>
      <form onSubmit={submit}>
        {error && <div className="alert">{error}</div>}
        {role === 'admin' && (
          <Field label="Patient">
            <select required value={f.patientId} onChange={(e) => setF({ ...f, patientId: e.target.value })}>
              <option value="">Select patient…</option>
              {(patients.data || []).map((p) => <option key={p.id} value={p.id}>{p.name} ({p.id})</option>)}
            </select>
          </Field>
        )}
        <Field label="Doctor">
          <select required value={f.doctorId} onChange={(e) => setF({ ...f, doctorId: e.target.value, time: '' })}>
            <option value="">Select doctor…</option>
            {(doctors.data || []).map((d) => <option key={d.id} value={d.id}>Dr. {d.name} — {d.specialization}</option>)}
          </select>
        </Field>
        <Field label="Date">
          <input type="date" required min={todayStr()} value={f.date} onChange={(e) => setF({ ...f, date: e.target.value, time: '' })} />
        </Field>
        <Field label="Available time slots">
          {f.doctorId ? (
            <div className="slots">
              {slots.length === 0 && <span className="sub-text">No free slots on this date.</span>}
              {slots.map((s) => (
                <button type="button" key={s} className={'slot' + (f.time === s ? ' active' : '')} onClick={() => setF({ ...f, time: s })}>{s}</button>
              ))}
            </div>
          ) : <span className="sub-text">Choose a doctor first.</span>}
        </Field>
        <button className="btn" disabled={busy}>{busy ? 'Booking…' : 'Confirm booking'}</button>
      </form>
    </Modal>
  );
}

function CompleteModal({ appt, onClose, onDone }) {
  const [f, setF] = useState({ diagnosis: '', prescription: '' });
  const { run, busy, error } = useAction();
  const submit = async (e) => {
    e.preventDefault();
    const r = await run(() => api.appointmentAction(appt.id, 'complete', f));
    if (r) onDone(r);
  };
  return (
    <Modal title={`Complete visit — ${appt.patientName}`} onClose={onClose}>
      <form onSubmit={submit}>
        {error && <div className="alert">{error}</div>}
        <Field label="Diagnosis"><textarea required rows={3} value={f.diagnosis} onChange={(e) => setF({ ...f, diagnosis: e.target.value })} /></Field>
        <Field label="Prescription"><textarea required rows={3} value={f.prescription} onChange={(e) => setF({ ...f, prescription: e.target.value })} /></Field>
        <p className="sub-text">A bill for your consultation fee is generated automatically.</p>
        <button className="btn success" disabled={busy}>{busy ? 'Saving…' : 'Save & complete'}</button>
      </form>
    </Modal>
  );
}

export default function Appointments({ role, user }) {
  const list = useLoad(api.appointments);
  const toast = useToast();
  const [booking, setBooking] = useState(false);
  const [completing, setCompleting] = useState(null);
  const [filter, setFilter] = useState('ALL');

  const act = async (id, action) => {
    try { await api.appointmentAction(id, action); toast(`Appointment ${id} ${action === 'confirm' ? 'confirmed' : 'cancelled'}`); list.reload(); }
    catch (e) { toast(e.message, 'err'); }
  };

  const rows = (list.data || []).filter((a) => filter === 'ALL' || a.status === filter);
  const cols = [
    { label: 'ID', key: 'id' },
    { label: 'Date & time', render: (a) => <>{prettyDate(a.date)} <span className="sub-text">{a.time}</span></> },
    role !== 'patient' && { label: 'Patient', render: (a) => <>{a.patientName}<div className="sub-text">{a.patientId}</div></> },
    role !== 'doctor' && { label: 'Doctor', render: (a) => <>Dr. {a.doctorName}<div className="sub-text">{a.specialization}</div></> },
    { label: 'Status', render: (a) => <Badge status={a.status} /> },
    { label: 'Actions', render: (a) => (
      <div className="actions">
        {role !== 'patient' && a.status === 'PENDING' && <button className="btn sm" onClick={() => act(a.id, 'confirm')}>Confirm</button>}
        {role === 'doctor' && a.status === 'CONFIRMED' && <button className="btn sm success" onClick={() => setCompleting(a)}>Complete</button>}
        {(a.status === 'PENDING' || a.status === 'CONFIRMED') && <button className="btn danger sm" onClick={() => act(a.id, 'cancel')}>Cancel</button>}
      </div>) },
  ].filter(Boolean);

  return (
    <>
      <Card flush title="Appointments" actions={
        <div className="toolbar">
          <select className="search" value={filter} onChange={(e) => setFilter(e.target.value)} style={{ minWidth: 150 }}>
            <option value="ALL">All statuses</option>
            {['PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'].map((s) => <option key={s} value={s}>{s[0] + s.slice(1).toLowerCase()}</option>)}
          </select>
          {role !== 'doctor' && <button className="btn" onClick={() => setBooking(true)}>+ Book appointment</button>}
        </div>}>
        <State loading={list.loading} error={list.error}><Table rows={rows} cols={cols} empty="No appointments found." /></State>
      </Card>
      {booking && <BookModal role={role} user={user} onClose={() => setBooking(false)} onDone={() => { setBooking(false); list.reload(); }} />}
      {completing && <CompleteModal appt={completing} onClose={() => setCompleting(null)}
        onDone={(r) => { setCompleting(null); toast(`Visit completed. Bill ${r.bill.id} generated.`); list.reload(); }} />}
    </>
  );
}
