package hospital.model;

import hospital.exception.HospitalException;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Appointment between one Patient and one Doctor (ASSOCIATION).
 * It controls its own life-cycle (state machine):
 *   PENDING -> CONFIRMED -> COMPLETED
 *   PENDING / CONFIRMED -> CANCELLED
 */
public class Appointment implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final Patient patient;
    private final Doctor doctor;
    private final LocalDate date;
    private final String time;
    private AppointmentStatus status = AppointmentStatus.PENDING;
    private final LocalDateTime createdAt = LocalDateTime.now();

    public Appointment(String id, Patient patient, Doctor doctor, LocalDate date, String time) {
        this.id = id;
        this.patient = patient;
        this.doctor = doctor;
        this.date = date;
        this.time = time;
    }

    public String getId() { return id; }
    public Patient getPatient() { return patient; }
    public Doctor getDoctor() { return doctor; }
    public LocalDate getDate() { return date; }
    public String getTime() { return time; }
    public AppointmentStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void confirm() throws HospitalException {
        if (status != AppointmentStatus.PENDING) {
            throw new HospitalException("Only a PENDING appointment can be confirmed (current: " + status + ").");
        }
        status = AppointmentStatus.CONFIRMED;
    }

    public void cancel() throws HospitalException {
        if (status == AppointmentStatus.COMPLETED || status == AppointmentStatus.CANCELLED) {
            throw new HospitalException("A " + status + " appointment cannot be cancelled.");
        }
        status = AppointmentStatus.CANCELLED;
    }

    public void complete() throws HospitalException {
        if (status != AppointmentStatus.CONFIRMED) {
            throw new HospitalException("Only a CONFIRMED appointment can be completed (current: " + status + ").");
        }
        status = AppointmentStatus.COMPLETED;
    }

    /** True if this appointment still blocks the doctor's time slot. */
    public boolean isActive() {
        return status == AppointmentStatus.PENDING || status == AppointmentStatus.CONFIRMED;
    }

    @Override
    public String toString() {
        return String.format("%-5s | %s %s | Patient: %-15s | Doctor: %-15s | %s",
                id, date, time, patient.getName(), doctor.getName(), status);
    }
}
