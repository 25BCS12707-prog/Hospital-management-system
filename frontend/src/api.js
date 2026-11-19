// Every call goes to the Java backend (/api/...). The session token is kept per browser tab.
const KEY = 'hms_session';

export const session = {
  get() { try { return JSON.parse(sessionStorage.getItem(KEY)); } catch { return null; } },
  set(v) { sessionStorage.setItem(KEY, JSON.stringify(v)); },
  clear() { sessionStorage.removeItem(KEY); },
};

async function request(method, path, body) {
  const s = session.get();
  const res = await fetch('/api' + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(s ? { Authorization: 'Bearer ' + s.token } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  }).catch(() => { throw new Error('Cannot reach the Java server. Is it running on port 8080?'); });
  const data = await res.json().catch(() => ({}));
  if (res.status === 401 && s) { session.clear(); window.location.reload(); }
  if (!res.ok) throw new Error(data.error || 'Request failed (' + res.status + ')');
  return data;
}

export const api = {
  login: (role, body) => request('POST', '/login/' + role, body),
  register: (role, body) => request('POST', '/register/' + role, body),
  logout: () => request('POST', '/logout', {}),
  stats: () => request('GET', '/stats'),
  patients: () => request('GET', '/patients'),
  patient: (id) => request('GET', '/patients/' + id),
  doctors: () => request('GET', '/doctors'),
  slots: (doctorId, date) => request('GET', `/doctors/${doctorId}/slots?date=${date}`),
  appointments: () => request('GET', '/appointments'),
  book: (body) => request('POST', '/appointments', body),
  appointmentAction: (id, action, body = {}) => request('POST', `/appointments/${id}/${action}`, body),
  bills: () => request('GET', '/bills'),
  createBill: (body) => request('POST', '/bills', body),
  payBill: (id) => request('POST', `/bills/${id}/pay`, {}),
  treatments: () => request('GET', '/treatments'),
};
