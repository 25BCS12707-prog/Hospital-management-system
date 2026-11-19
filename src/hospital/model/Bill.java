package hospital.model;

import hospital.exception.HospitalException;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Bill implements the Payable INTERFACE. */
public class Bill implements Payable, Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final Patient patient;
    private final double amount;
    private final String description;
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;
    private final LocalDateTime createdAt = LocalDateTime.now();

    public Bill(String id, Patient patient, double amount, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Bill amount must be greater than 0.");
        }
        this.id = id;
        this.patient = patient;
        this.amount = amount;
        this.description = description;
    }

    public String getId() { return id; }
    public Patient getPatient() { return patient; }
    public String getDescription() { return description; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }

    @Override
    public double getAmount() { return amount; }

    @Override
    public boolean isPaid() { return paymentStatus == PaymentStatus.PAID; }

    @Override
    public void pay() throws HospitalException {
        if (isPaid()) {
            throw new HospitalException("Bill " + id + " is already paid.");
        }
        paymentStatus = PaymentStatus.PAID;
    }

    @Override
    public String toString() {
        return String.format("%-5s | %s | Patient: %-15s | Rs.%9.2f | %-6s | %s",
                id, createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                patient.getName(), amount, paymentStatus, description);
    }
}
