import { api } from '../api.js';
import { Badge, Card, Stat, State, Table, money, prettyDate, todayStr, useLoad } from '../ui.jsx';

const apptCols = (role) => [
  { label: 'Date', render: (a) => <>{prettyDate(a.date)} <span className="sub-text">{a.time}</span></> },
  role !== 'patient' && { label: 'Patient', key: 'patientName' },
  role !== 'doctor' && { label: 'Doctor', render: (a) => <>{a.doctorName}<div className="sub-text">{a.specialization}</div></> },
  { label: 'Status', render: (a) => <Badge status={a.status} /> },
].filter(Boolean);

function Bars({ items }) {
  const max = Math.max(1, ...items.map((i) => i.value));
  return items.map((i) => (
    <div className="bar-row" key={i.label}>
      <span>{i.label}</span><div className="bar"><span style={{ width: (i.value / max) * 100 + '%' }} /></div><strong>{i.value}</strong>
    </div>
  ));
}

function Admin({ go }) {
  const s = useLoad(api.stats);
  const a = useLoad(api.appointments);
  const st = s.data;
  return (
    <State loading={s.loading || a.loading} error={s.error || a.error}>
      {st && (<>
        <div className="stats">
          <Stat icon="🧑‍🤝‍🧑" value={st.patients} label="Patients" />
          <Stat icon="🩺" value={st.doctors} label="Doctors" />
          <Stat icon="📅" value={st.appointments} label="Appointments" />
          <Stat icon="💰" value={money(st.revenue)} label="Revenue collected" />
          <Stat icon="⏳" value={money(st.pendingAmount)} label="Pending payments" />
        </div>
        <div className="grid-2">
          <Card title="Appointments by status">
            <Bars items={[
              { label: 'Pending', value: st.pendingCount }, { label: 'Confirmed', value: st.confirmedCount },
              { label: 'Completed', value: st.completedCount }, { label: 'Cancelled', value: st.cancelledCount }]} />
          </Card>
          <Card title="Quick actions">
            <div className="actions">
              <button className="btn" onClick={() => go('appointments')}>Book appointment</button>
              <button className="btn secondary" onClick={() => go('patients')}>Manage patients</button>
              <button className="btn secondary" onClick={() => go('doctors')}>Manage doctors</button>
              <button className="btn secondary" onClick={() => go('billing')}>Billing</button>
            </div>
          </Card>
        </div>
        <Card title="Recent appointments" flush>
          <Table cols={apptCols('admin')} rows={(a.data || []).slice(0, 6)} empty="No appointments booked yet." />
        </Card>
      </>)}
    </State>
  );
}

function Doctor({ user, go }) {
  const a = useLoad(api.appointments);
  const list = a.data || [];
  const n = (s) => list.filter((x) => x.status === s).length;
  const today = todayStr();
  const patients = new Set(list.map((x) => x.patientId)).size;
  return (
    <State loading={a.loading} error={a.error}>
      <div className="stats">
        <Stat icon="📅" value={list.length} label="Total appointments" />
        <Stat icon="⏳" value={n('PENDING')} label="Awaiting confirmation" />
        <Stat icon="✅" value={n('CONFIRMED')} label="Confirmed" />
        <Stat icon="🧑‍🤝‍🧑" value={patients} label="Patients seen / booked" />
        <Stat icon="💳" value={money(user.fee)} label="Your consultation fee" />
      </div>
      <Card title={`Upcoming for Dr. ${user.name}`} actions={<button className="btn sm" onClick={() => go('appointments')}>Manage</button>} flush>
        <Table cols={apptCols('doctor')}
          rows={list.filter((x) => x.date >= today && (x.status === 'PENDING' || x.status === 'CONFIRMED')).reverse().slice(0, 6)}
          empty="No upcoming appointments." />
      </Card>
    </State>
  );
}

function Patient({ user, go }) {
  const a = useLoad(api.appointments);
  const b = useLoad(api.bills);
  const list = a.data || [];
  const unpaid = (b.data || []).filter((x) => x.status === 'UNPAID');
  const today = todayStr();
  return (
    <State loading={a.loading || b.loading} error={a.error || b.error}>
      <div className="stats">
        <Stat icon="📅" value={list.filter((x) => x.status === 'PENDING' || x.status === 'CONFIRMED').length} label="Active appointments" />
        <Stat icon="✅" value={list.filter((x) => x.status === 'COMPLETED').length} label="Completed visits" />
        <Stat icon="⏳" value={money(unpaid.reduce((s, x) => s + x.amount, 0))} label={`Outstanding (${unpaid.length} bills)`} />
      </div>
      <Card title={`Hello, ${user.name}`} actions={<button className="btn" onClick={() => go('doctors')}>Book appointment</button>}>
        <div className="sub-text">Patient ID {user.id} · {user.age} yrs · {user.gender[0] + user.gender.slice(1).toLowerCase()}</div>
      </Card>
      <Card title="Your upcoming appointments" flush>
        <Table cols={apptCols('patient')}
          rows={list.filter((x) => x.date >= today && (x.status === 'PENDING' || x.status === 'CONFIRMED')).reverse()}
          empty="No upcoming appointments." />
      </Card>
    </State>
  );
}

export default function Dashboard(props) {
  if (props.role === 'admin') return <Admin {...props} />;
  if (props.role === 'doctor') return <Doctor {...props} />;
  return <Patient {...props} />;
}
