import { useState } from 'react';
import { api } from '../api.js';
import { Badge, Card, Field, Modal, Stat, State, Table, money, useAction, useLoad, useToast } from '../ui.jsx';

function CreateBill({ onClose, onDone }) {
  const patients = useLoad(api.patients);
  const [f, setF] = useState({ patientId: '', amount: '', description: '' });
  const { run, busy, error } = useAction();
  const submit = async (e) => {
    e.preventDefault();
    const b = await run(() => api.createBill(f));
    if (b) onDone(b);
  };
  return (
    <Modal title="Create bill" onClose={onClose}>
      <form onSubmit={submit}>
        {error && <div className="alert">{error}</div>}
        <Field label="Patient">
          <select required value={f.patientId} onChange={(e) => setF({ ...f, patientId: e.target.value })}>
            <option value="">Select patient…</option>
            {(patients.data || []).map((p) => <option key={p.id} value={p.id}>{p.name} ({p.id})</option>)}
          </select>
        </Field>
        <Field label="Amount (₹)"><input type="number" min="1" step="0.01" required value={f.amount} onChange={(e) => setF({ ...f, amount: e.target.value })} /></Field>
        <Field label="Description"><input required value={f.description} onChange={(e) => setF({ ...f, description: e.target.value })} placeholder="e.g. Lab test, X-ray, room charges" /></Field>
        <button className="btn" disabled={busy}>{busy ? 'Saving…' : 'Create bill'}</button>
      </form>
    </Modal>
  );
}

export default function Billing({ role }) {
  const list = useLoad(api.bills);
  const toast = useToast();
  const [creating, setCreating] = useState(false);
  const bills = list.data || [];
  const sum = (s) => bills.filter((b) => b.status === s).reduce((t, b) => t + b.amount, 0);

  const pay = async (b) => {
    if (!window.confirm(`Mark bill ${b.id} (${money(b.amount)}) as paid?`)) return;
    try { await api.payBill(b.id); toast(`Bill ${b.id} paid`); list.reload(); }
    catch (e) { toast(e.message, 'err'); }
  };

  const cols = [
    { label: 'Bill', key: 'id' },
    role === 'admin' && { label: 'Patient', render: (b) => <>{b.patientName}<div className="sub-text">{b.patientId}</div></> },
    { label: 'Description', key: 'description' },
    { label: 'Amount', render: (b) => <strong>{money(b.amount)}</strong> },
    { label: 'Status', render: (b) => <Badge status={b.status} /> },
    { label: '', render: (b) => b.status === 'UNPAID' && (
      <button className="btn sm" onClick={() => pay(b)}>{role === 'admin' ? 'Mark paid' : 'Pay now'}</button>) },
  ].filter(Boolean);

  return (
    <>
      <div className="stats">
        <Stat icon="🧾" value={bills.length} label="Total bills" />
        <Stat icon="✅" value={money(sum('PAID'))} label={role === 'admin' ? 'Collected' : 'Paid'} />
        <Stat icon="⏳" value={money(sum('UNPAID'))} label="Outstanding" />
      </div>
      <Card flush title={role === 'admin' ? 'All bills' : 'Your bills'}
        actions={role === 'admin' && <button className="btn" onClick={() => setCreating(true)}>+ Create bill</button>}>
        <State loading={list.loading} error={list.error}><Table rows={bills} cols={cols} empty="No bills yet. Bills are created automatically when a doctor completes an appointment." /></State>
      </Card>
      {creating && <CreateBill onClose={() => setCreating(false)} onDone={(b) => { setCreating(false); toast(`Bill ${b.id} created`); list.reload(); }} />}
    </>
  );
}
