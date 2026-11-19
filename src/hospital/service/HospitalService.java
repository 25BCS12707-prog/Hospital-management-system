package hospital.service;

import hospital.exception.HospitalException;
import hospital.model.*;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Business-logic layer. Holds all data and enforces the hospital's rules.
 * The whole object is Serializable, so save() / load() persist everything to a file.
 */
public class HospitalService implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Admin admin =
            new Admin("ADM01", "Hospital Admin", "9999999999", "admin@hospital.com", "admin123");

    private final Map<String, Patient> patients = new LinkedHashMap<>();
    private final Map<String, Doctor> doctors = new LinkedHashMap<>();
    private final Map<String, Appointment> appointments = new LinkedHashMap<>();
    private final Map<String, Treatment> treatments = new LinkedHashMap<>();
    private final Map<String, Bill> bills = new LinkedHashMap<>();

    private int patientSeq, doctorSeq, appointmentSeq, treatmentSeq, billSeq;

    private static String makeId(String prefix, int number) {
        return String.format("%s%03d", prefix, number);
    }

    // ------------------------------------------------------------ authentication
    public Admin loginAdmin(String email, String password) throws HospitalException {
        if (!admin.authenticate(email, password)) {
            throw new HospitalException("Invalid admin email or password.");
        }
        return admin;
    }

    public Patient findPatient(String key) throws HospitalException {
        return patients.values().stream()
                .filter(p -> p.getId().equalsIgnoreCase(key.trim()) || p.getEmail().equalsIgnoreCase(key.trim()))
                .findFirst()
                .orElseThrow(() -> new HospitalException("No patient found for '" + key + "'."));
    }

    public Doctor findDoctor(String key) throws HospitalException {
        return doctors.values().stream()
                .filter(d -> d.getId().equalsIgnoreCase(key.trim()) || d.getEmail().equalsIgnoreCase(key.trim()))
                .findFirst()
                .orElseThrow(() -> new HospitalException("No doctor found for '" + key + "'."));
    }

    // ------------------------------------------------------------ registration
    public Patient registerPatient(String name, int age, Gender gender, String phone, String email)
            throws HospitalException {
        ensureEmailFree(email);
        Patient p = new Patient(makeId("P", patientSeq + 1), name, age, gender, phone, email);
        patientSeq++; // consume the ID only if validation passed
        patients.put(p.getId(), p);
        return p;
    }

    public Doctor registerDoctor(String name, String specialization, double fee, String phone, String email)
            throws HospitalException {
        ensureEmailFree(email);
        Doctor d = new Doctor(makeId("D", doctorSeq + 1), name, specialization, fee, phone, email);
        doctorSeq++; // consume the ID only if validation passed
        doctors.put(d.getId(), d);
        return d;
    }

    /** Polymorphism: Patient and Doctor are both treated as Person here. */
    private void ensureEmailFree(String email) throws HospitalException {
        List<Person> everyone = new ArrayList<>();
        everyone.addAll(patients.values());
        everyone.addAll(doctors.values());
        for (Person p : everyone) {
            if (p.getEmail().equalsIgnoreCase(email.trim())) {
                throw new HospitalException("Email already registered to " + p.getRole() + " " + p.getId() + ".");
            }
        }
    }

    // ------------------------------------------------------------ appointments
    public List<String> getFreeSlots(Doctor doctor, LocalDate date) {
        Set<String> taken = appointments.values().stream()
                .filter(a -> a.isActive() && a.getDoctor().equals(doctor) && a.getDate().equals(date))
                .map(Appointment::getTime)
                .collect(Collectors.toSet());
        return doctor.getAvailableSlots().stream().filter(s -> !taken.contains(s)).collect(Collectors.toList());
    }

    public Appointment bookAppointment(Patient patient, Doctor doctor, LocalDate date, String time)
            throws HospitalException {
        if (date.isBefore(LocalDate.now())) {
            throw new HospitalException("Cannot book an appointment in the past.");
        }
        if (!getFreeSlots(doctor, date).contains(time)) {
            throw new HospitalException("Slot " + time + " is not available for " + doctor.getName() + " on " + date + ".");
        }
        Appointment a = new Appointment(makeId("A", ++appointmentSeq), patient, doctor, date, time);
        appointments.put(a.getId(), a);
        return a;
    }

    /**
     * Finds an appointment and checks that the given person (patient OR doctor)
     * is really part of it. Takes the base type Person -> POLYMORPHISM.
     */
    public Appointment findAppointmentOf(Person owner, String id) throws HospitalException {
        Appointment a = appointments.get(id.trim().toUpperCase());
        boolean ownedByPerson = a != null
                && (a.getPatient().getId().equals(owner.getId()) || a.getDoctor().getId().equals(owner.getId()));
        boolean isAdmin = owner instanceof Admin;
        if (a == null || !(ownedByPerson || isAdmin)) {
            throw new HospitalException("Appointment '" + id + "' not found for you.");
        }
        return a;
    }

    public List<Appointment> getAllAppointments() {
        return new ArrayList<>(appointments.values());
    }

    public List<Appointment> getAppointmentsOf(Person person) {
        return appointments.values().stream()
                .filter(a -> a.getPatient().getId().equals(person.getId()) || a.getDoctor().getId().equals(person.getId()))
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------ treatment + automatic billing
    /** Completes a confirmed appointment, stores the treatment and auto-generates the bill. */
    public Bill completeAppointment(Appointment appt, String diagnosis, String prescription)
            throws HospitalException {
        if (diagnosis.isBlank() || prescription.isBlank()) {
            throw new HospitalException("Diagnosis and prescription are required.");
        }
        appt.complete(); // throws if not CONFIRMED
        Treatment t = new Treatment(makeId("T", ++treatmentSeq), appt, diagnosis.trim(), prescription.trim());
        treatments.put(t.getId(), t);
        appt.getPatient().addMedicalRecord(appt.getDate() + ": " + diagnosis.trim());
        return addBill(appt.getPatient(), appt.getDoctor().getConsultationFee(),
                "Consultation - Dr. " + appt.getDoctor().getName());
    }

    public List<Treatment> getTreatmentsOf(Patient patient) {
        return treatments.values().stream()
                .filter(t -> t.getAppointment().getPatient().equals(patient))
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------ billing
    public Bill addBill(Patient patient, double amount, String description) {
        Bill b = new Bill(makeId("B", billSeq + 1), patient, amount, description);
        billSeq++; // consume the ID only if validation passed
        bills.put(b.getId(), b);
        return b;
    }

    public List<Bill> getAllBills() {
        return new ArrayList<>(bills.values());
    }

    public List<Bill> getBillsOf(Patient patient) {
        return bills.values().stream().filter(b -> b.getPatient().equals(patient)).collect(Collectors.toList());
    }

    public Bill payBill(Patient patient, String billId) throws HospitalException {
        Bill b = bills.get(billId.trim().toUpperCase());
        if (b == null || !b.getPatient().equals(patient)) {
            throw new HospitalException("Bill '" + billId + "' not found for you.");
        }
        b.pay();
        return b;
    }

    // ------------------------------------------------------------ dashboard
    public Collection<Patient> getAllPatients() { return patients.values(); }
    public Collection<Doctor> getAllDoctors() { return doctors.values(); }

    /** Works on any Payable (interface) - not only Bill. */
    private static double sum(Collection<? extends Payable> items, boolean paid) {
        return items.stream().filter(p -> p.isPaid() == paid).mapToDouble(Payable::getAmount).sum();
    }

    public double getRevenue() { return sum(bills.values(), true); }
    public double getPendingAmount() { return sum(bills.values(), false); }

    public long countByStatus(AppointmentStatus status) {
        return appointments.values().stream().filter(a -> a.getStatus() == status).count();
    }

    // ------------------------------------------------------------ file persistence
    public void save(String path) throws HospitalException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path))) {
            out.writeObject(this);
        } catch (IOException e) {
            throw new HospitalException("Could not save data: " + e.getMessage());
        }
    }

    public static HospitalService load(String path) {
        File f = new File(path);
        if (f.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
                return (HospitalService) in.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Saved data could not be read, starting fresh. (" + e.getMessage() + ")");
            }
        }
        HospitalService fresh = new HospitalService();
        fresh.seedSampleData();
        return fresh;
    }

    private void seedSampleData() {
        try {
            registerDoctor("Anil Sharma", "Cardiology", 800, "9876500001", "anil@hospital.com");
            registerDoctor("Neha Gupta", "Dermatology", 600, "9876500002", "neha@hospital.com");
            registerDoctor("Rakesh Verma", "General Medicine", 400, "9876500003", "rakesh@hospital.com");
            registerPatient("Simran Kaur", 24, Gender.FEMALE, "9812300001", "simran@mail.com");
            registerPatient("Rohit Singh", 35, Gender.MALE, "9812300002", "rohit@mail.com");
        } catch (HospitalException e) {
            throw new IllegalStateException(e);
        }
    }
}
