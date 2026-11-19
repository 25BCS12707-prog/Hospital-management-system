package hospital.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Diagnosis + prescription written by a doctor for a completed appointment. */
public class Treatment implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final Appointment appointment;
    private final String diagnosis;
    private final String prescription;
    private final LocalDateTime date = LocalDateTime.now();

    public Treatment(String id, Appointment appointment, String diagnosis, String prescription) {
        this.id = id;
        this.appointment = appointment;
        this.diagnosis = diagnosis;
        this.prescription = prescription;
    }

    public String getId() { return id; }
    public Appointment getAppointment() { return appointment; }
    public String getDiagnosis() { return diagnosis; }
    public String getPrescription() { return prescription; }

    @Override
    public String toString() {
        return String.format("[%s] %s | Dr. %s%n      Diagnosis   : %s%n      Prescription: %s",
                id, date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                appointment.getDoctor().getName(), diagnosis, prescription);
    }
}
