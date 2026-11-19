package hospital.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import hospital.exception.HospitalException;
import hospital.model.*;
import hospital.service.HospitalService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Thin HTTP/JSON layer over the existing HospitalService (JDK built-in server, no libraries).
 * All business rules stay in HospitalService / the model classes.
 */
public class ApiServer {
    private static final String DATA_FILE = "hospital.dat";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final HospitalService service = HospitalService.load(DATA_FILE);
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Path staticDir;

    private record Session(String role, String id) {}

    private static class ApiError extends Exception {
        final int status;
        ApiError(int status, String msg) { super(msg); this.status = status; }
    }

    public ApiServer(Path staticDir) { this.staticDir = staticDir; }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Path dist = Path.of("frontend", "dist");
        ApiServer app = new ApiServer(dist);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", app::handle);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("Hospital API running on http://localhost:" + port);
        if (Files.isDirectory(dist)) System.out.println("Serving built frontend from " + dist.toAbsolutePath());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> app.persist()));
    }

    private synchronized void persist() {
        try { service.save(DATA_FILE); } catch (HospitalException e) { System.err.println(e.getMessage()); }
    }

    // ------------------------------------------------------------------ routing
    private void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        try {
            if (!path.startsWith("/api/")) { serveStatic(ex, path); return; }
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Object result;
            int status = 200;
            synchronized (this) {
                try {
                    result = route(ex.getRequestMethod(), path.substring(5).split("/"),
                            Json.parseObject(body), query(ex), session(ex));
                    if (!ex.getRequestMethod().equals("GET")) persist();
                } catch (ApiError e) {
                    status = e.status; result = Map.of("error", e.getMessage());
                } catch (HospitalException | IllegalArgumentException | DateTimeParseException | ClassCastException e) {
                    status = 400; result = Map.of("error", friendly(e));
                }
            }
            send(ex, status, "application/json", Json.write(result).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, "application/json", "{\"error\":\"Internal server error\"}".getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String friendly(Exception e) {
        if (e instanceof DateTimeParseException) return "Invalid date (use YYYY-MM-DD).";
        if (e instanceof ClassCastException) return "Invalid input.";
        return e.getMessage();
    }

    private Object route(String m, String[] p, Map<String, Object> b, Map<String, String> q, Session s)
            throws ApiError, HospitalException {
        String a = p[0];
        // ---- public
        if (m.equals("POST") && a.equals("login") && p.length == 2) return login(p[1], b);
        if (m.equals("POST") && a.equals("register") && p.length == 2) return register(p[1], b);
        if (s == null) throw new ApiError(401, "Please log in first.");

        switch (a) {
            case "logout" -> { sessions.values().remove(s); return Map.of("ok", true); }
            case "me" -> { return me(s); }
            case "stats" -> { return stats(s); }
            case "patients" -> { return patients(m, p, s); }
            case "doctors" -> { return doctors(m, p, q, s); }
            case "appointments" -> { return appointments(m, p, b, s); }
            case "bills" -> { return bills(m, p, b, s); }
            case "treatments" -> { return treatments(s); }
            default -> throw new ApiError(404, "Unknown endpoint.");
        }
    }

    // ------------------------------------------------------------------ auth
    private Object login(String role, Map<String, Object> b) throws HospitalException {
        Session s;
        Map<String, Object> user;
        switch (role) {
            case "admin" -> {
                Admin ad = service.loginAdmin(str(b, "email"), str(b, "password"));
                s = new Session("admin", ad.getId());
                user = personJson(ad);
            }
            case "doctor" -> {
                Doctor d = service.findDoctor(str(b, "key"));
                s = new Session("doctor", d.getId());
                user = doctorJson(d);
            }
            case "patient" -> {
                Patient pt = service.findPatient(str(b, "key"));
                s = new Session("patient", pt.getId());
                user = patientJson(pt);
            }
            default -> throw new HospitalException("Unknown role.");
        }
        String token = UUID.randomUUID().toString();
        sessions.put(token, s);
        return Map.of("token", token, "role", s.role(), "user", user);
    }

    private Object register(String role, Map<String, Object> b) throws HospitalException {
        if (role.equals("patient")) {
            Patient p = service.registerPatient(str(b, "name"), intVal(b, "age"),
                    Gender.valueOf(str(b, "gender").toUpperCase()), str(b, "phone"), str(b, "email"));
            return patientJson(p);
        }
        if (role.equals("doctor")) {
            Doctor d = service.registerDoctor(str(b, "name"), str(b, "specialization"),
                    num(b, "fee"), str(b, "phone"), str(b, "email"));
            return doctorJson(d);
        }
        throw new HospitalException("Unknown role.");
    }

    private Object me(Session s) throws HospitalException {
        Map<String, Object> user = switch (s.role()) {
            case "doctor" -> doctorJson(service.findDoctor(s.id()));
            case "patient" -> patientJson(service.findPatient(s.id()));
            default -> personJson(adminOf());
        };
        return Map.of("role", s.role(), "user", user);
    }

    private Admin adminOf() throws HospitalException { return service.loginAdmin("admin@hospital.com", "admin123"); }

    // ------------------------------------------------------------------ resources
    private Object stats(Session s) throws ApiError {
        need(s, "admin");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("patients", service.getAllPatients().size());
        m.put("doctors", service.getAllDoctors().size());
        m.put("appointments", service.getAllAppointments().size());
        for (AppointmentStatus st : AppointmentStatus.values()) m.put(st.name().toLowerCase() + "Count", service.countByStatus(st));
        m.put("revenue", service.getRevenue());
        m.put("pendingAmount", service.getPendingAmount());
        m.put("bills", service.getAllBills().size());
        return m;
    }

    private Object patients(String m, String[] p, Session s) throws ApiError, HospitalException {
        if (s.role().equals("patient")) throw new ApiError(403, "Not allowed.");
        if (p.length == 1) {
            List<Object> out = new ArrayList<>();
            if (s.role().equals("doctor")) {
                Doctor d = service.findDoctor(s.id());
                Set<String> seen = new LinkedHashSet<>();
                for (Appointment a : service.getAppointmentsOf(d)) {
                    if (seen.add(a.getPatient().getId())) out.add(patientJson(a.getPatient()));
                }
            } else for (Patient pt : service.getAllPatients()) out.add(patientJson(pt));
            return out;
        }
        Patient pt = service.findPatient(p[1]);
        Map<String, Object> j = patientJson(pt);
        j.put("history", pt.getMedicalHistory());
        List<Object> tr = new ArrayList<>();
        for (Treatment t : service.getTreatmentsOf(pt)) tr.add(treatmentJson(t));
        j.put("treatments", tr);
        return j;
    }

    private Object doctors(String m, String[] p, Map<String, String> q, Session s) throws ApiError, HospitalException {
        if (p.length == 3 && p[2].equals("slots")) {
            Doctor d = service.findDoctor(p[1]);
            LocalDate date = LocalDate.parse(q.getOrDefault("date", ""), DAY);
            return service.getFreeSlots(d, date);
        }
        List<Object> out = new ArrayList<>();
        for (Doctor d : service.getAllDoctors()) out.add(doctorJson(d));
        return out;
    }

    private Object appointments(String m, String[] p, Map<String, Object> b, Session s) throws ApiError, HospitalException {
        if (p.length == 1 && m.equals("GET")) {
            List<Appointment> list = switch (s.role()) {
                case "admin" -> service.getAllAppointments();
                case "doctor" -> service.getAppointmentsOf(service.findDoctor(s.id()));
                default -> service.getAppointmentsOf(service.findPatient(s.id()));
            };
            List<Object> out = new ArrayList<>();
            for (Appointment a : list) out.add(apptJson(a));
            Collections.reverse(out);
            return out;
        }
        if (p.length == 1 && m.equals("POST")) {
            if (s.role().equals("doctor")) throw new ApiError(403, "Doctors cannot book appointments.");
            Patient pt = s.role().equals("patient") ? service.findPatient(s.id()) : service.findPatient(str(b, "patientId"));
            Doctor d = service.findDoctor(str(b, "doctorId"));
            return apptJson(service.bookAppointment(pt, d, LocalDate.parse(str(b, "date"), DAY), str(b, "time")));
        }
        if (p.length == 3 && m.equals("POST")) {
            Person owner = switch (s.role()) {
                case "admin" -> adminOf();
                case "doctor" -> service.findDoctor(s.id());
                default -> service.findPatient(s.id());
            };
            Appointment a = service.findAppointmentOf(owner, p[1]);
            switch (p[2]) {
                case "confirm" -> { if (s.role().equals("patient")) throw new ApiError(403, "Not allowed."); a.confirm(); }
                case "cancel" -> a.cancel();
                case "complete" -> {
                    if (!s.role().equals("doctor")) throw new ApiError(403, "Only the doctor can complete an appointment.");
                    Bill bill = service.completeAppointment(a, str(b, "diagnosis"), str(b, "prescription"));
                    Map<String, Object> r = apptJson(a);
                    r.put("bill", billJson(bill));
                    return r;
                }
                default -> throw new ApiError(404, "Unknown action.");
            }
            return apptJson(a);
        }
        throw new ApiError(404, "Unknown endpoint.");
    }

    private Object bills(String m, String[] p, Map<String, Object> b, Session s) throws ApiError, HospitalException {
        if (p.length == 1 && m.equals("GET")) {
            List<Bill> list = switch (s.role()) {
                case "admin" -> service.getAllBills();
                case "patient" -> service.getBillsOf(service.findPatient(s.id()));
                default -> throw new ApiError(403, "Not allowed.");
            };
            List<Object> out = new ArrayList<>();
            for (Bill x : list) out.add(billJson(x));
            Collections.reverse(out);
            return out;
        }
        if (p.length == 1 && m.equals("POST")) {
            need(s, "admin");
            return billJson(service.addBill(service.findPatient(str(b, "patientId")), num(b, "amount"), str(b, "description")));
        }
        if (p.length == 3 && m.equals("POST") && p[2].equals("pay")) {
            if (s.role().equals("doctor")) throw new ApiError(403, "Not allowed.");
            Patient owner = null;
            if (s.role().equals("patient")) owner = service.findPatient(s.id());
            else for (Bill x : service.getAllBills()) if (x.getId().equalsIgnoreCase(p[1])) owner = x.getPatient();
            if (owner == null) throw new HospitalException("Bill '" + p[1] + "' not found.");
            return billJson(service.payBill(owner, p[1]));
        }
        throw new ApiError(404, "Unknown endpoint.");
    }

    private Object treatments(Session s) throws ApiError, HospitalException {
        List<Object> out = new ArrayList<>();
        for (Patient pt : service.getAllPatients()) {
            if (s.role().equals("patient") && !pt.getId().equals(s.id())) continue;
            for (Treatment t : service.getTreatmentsOf(pt)) {
                if (s.role().equals("doctor") && !t.getAppointment().getDoctor().getId().equals(s.id())) continue;
                out.add(treatmentJson(t));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ JSON mapping
    private static Map<String, Object> personJson(Person p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId()); m.put("name", p.getName()); m.put("phone", p.getPhone());
        m.put("email", p.getEmail()); m.put("role", p.getRole());
        return m;
    }

    private static Map<String, Object> patientJson(Patient p) {
        Map<String, Object> m = personJson(p);
        m.put("age", p.getAge()); m.put("gender", p.getGender());
        return m;
    }

    private static Map<String, Object> doctorJson(Doctor d) {
        Map<String, Object> m = personJson(d);
        m.put("specialization", d.getSpecialization()); m.put("fee", d.getConsultationFee());
        m.put("slots", d.getAvailableSlots());
        return m;
    }

    private static Map<String, Object> apptJson(Appointment a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("patientId", a.getPatient().getId()); m.put("patientName", a.getPatient().getName());
        m.put("doctorId", a.getDoctor().getId()); m.put("doctorName", a.getDoctor().getName());
        m.put("specialization", a.getDoctor().getSpecialization());
        m.put("date", a.getDate().toString()); m.put("time", a.getTime());
        m.put("status", a.getStatus());
        return m;
    }

    private static Map<String, Object> billJson(Bill b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("patientId", b.getPatient().getId()); m.put("patientName", b.getPatient().getName());
        m.put("amount", b.getAmount()); m.put("description", b.getDescription());
        m.put("status", b.getPaymentStatus());
        return m;
    }

    private static Map<String, Object> treatmentJson(Treatment t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("appointmentId", t.getAppointment().getId());
        m.put("date", t.getAppointment().getDate().toString());
        m.put("patientId", t.getAppointment().getPatient().getId());
        m.put("patientName", t.getAppointment().getPatient().getName());
        m.put("doctorName", t.getAppointment().getDoctor().getName());
        m.put("diagnosis", t.getDiagnosis()); m.put("prescription", t.getPrescription());
        return m;
    }

    // ------------------------------------------------------------------ helpers
    private static void need(Session s, String role) throws ApiError {
        if (!s.role().equals(role)) throw new ApiError(403, "Not allowed for your role.");
    }

    private static String str(Map<String, Object> b, String k) {
        Object v = b.get(k);
        if (v == null) throw new IllegalArgumentException("Missing field: " + k);
        return v.toString();
    }

    private static double num(Map<String, Object> b, String k) {
        try { return Double.parseDouble(str(b, k)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("'" + k + "' must be a number."); }
    }

    private static int intVal(Map<String, Object> b, String k) { return (int) Math.round(num(b, k)); }

    private Session session(HttpExchange ex) {
        String h = ex.getRequestHeaders().getFirst("Authorization");
        return h != null && h.startsWith("Bearer ") ? sessions.get(h.substring(7)) : null;
    }

    private static Map<String, String> query(HttpExchange ex) {
        Map<String, String> m = new HashMap<>();
        String raw = ex.getRequestURI().getRawQuery();
        if (raw == null) return m;
        for (String kv : raw.split("&")) {
            String[] x = kv.split("=", 2);
            m.put(java.net.URLDecoder.decode(x[0], StandardCharsets.UTF_8),
                    x.length > 1 ? java.net.URLDecoder.decode(x[1], StandardCharsets.UTF_8) : "");
        }
        return m;
    }

    private void serveStatic(HttpExchange ex, String path) throws IOException {
        if (!Files.isDirectory(staticDir)) {
            send(ex, 200, "text/plain; charset=utf-8",
                    "Hospital API is running. Start the frontend with: cd frontend && npm run dev".getBytes(StandardCharsets.UTF_8));
            return;
        }
        Path root = staticDir.toAbsolutePath().normalize();
        Path f = root.resolve(path.equals("/") ? "index.html" : path.substring(1)).normalize();
        if (!f.startsWith(root) || !Files.isRegularFile(f)) f = root.resolve("index.html"); // SPA fallback
        String n = f.getFileName().toString();
        String type = n.endsWith(".html") ? "text/html; charset=utf-8" : n.endsWith(".js") ? "text/javascript"
                : n.endsWith(".css") ? "text/css" : n.endsWith(".svg") ? "image/svg+xml"
                : n.endsWith(".json") ? "application/json" : "application/octet-stream";
        send(ex, 200, type, Files.readAllBytes(f));
    }

    private static void send(HttpExchange ex, int status, String type, byte[] data) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type.startsWith("application/json") ? "application/json; charset=utf-8" : type);
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        if (ex.getRequestMethod().equals("OPTIONS")) { ex.sendResponseHeaders(204, -1); ex.close(); return; }
        ex.sendResponseHeaders(status, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }
}
