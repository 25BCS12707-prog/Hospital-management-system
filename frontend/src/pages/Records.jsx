import { api } from '../api.js';
import { Card, State, prettyDate, useLoad } from '../ui.jsx';

export default function Records({ role }) {
  const list = useLoad(api.treatments);
  const rows = [...(list.data || [])].reverse();
  return (
    <Card title={role === 'doctor' ? 'Treatments you have recorded' : 'Your medical history'}>
      <State loading={list.loading} error={list.error}>
        {rows.length === 0 && <div className="empty">No treatments recorded yet. They appear here after a doctor completes an appointment.</div>}
        {rows.map((t) => (
          <div className="record" key={t.id}>
            <strong>{prettyDate(t.date)}</strong> · {role === 'doctor' ? t.patientName : 'Dr. ' + t.doctorName}
            <span className="sub-text"> · Appointment {t.appointmentId}</span>
            <div><span className="sub-text">Diagnosis:</span> {t.diagnosis}</div>
            <div><span className="sub-text">Prescription:</span> {t.prescription}</div>
          </div>
        ))}
      </State>
    </Card>
  );
}
