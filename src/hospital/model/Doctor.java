package hospital.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** INHERITANCE: Doctor IS-A Person. */
public class Doctor extends Person {
    private static final long serialVersionUID = 1L;

    /** Default daily time slots of every doctor. */
    private static final List<String> DEFAULT_SLOTS =
            Arrays.asList("09:00", "10:00", "11:00", "12:00", "14:00", "15:00", "16:00");

    private String specialization;
    private double consultationFee;

    public Doctor(String id, String name, String specialization, double consultationFee,
                  String phone, String email) {
        super(id, name, phone, email);
        if (specialization == null || specialization.isBlank()) {
            throw new IllegalArgumentException("Specialization cannot be empty.");
        }
        if (consultationFee <= 0) {
            throw new IllegalArgumentException("Consultation fee must be greater than 0.");
        }
        this.specialization = specialization.trim();
        this.consultationFee = consultationFee;
    }

    public String getSpecialization() { return specialization; }
    public double getConsultationFee() { return consultationFee; }

    public List<String> getAvailableSlots() {
        return Collections.unmodifiableList(DEFAULT_SLOTS);
    }

    @Override
    public String getRole() {
        return "Doctor";
    }

    @Override
    public String getDetails() {
        return super.getDetails()
                + String.format(" | %s | Fee: Rs.%.2f", specialization, consultationFee);
    }
}
