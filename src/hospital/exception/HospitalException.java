package hospital.exception;

/**
 * Custom checked exception used for all business-rule violations
 * (invalid status change, slot already booked, record not found, ...).
 */
public class HospitalException extends Exception {
    private static final long serialVersionUID = 1L;

    public HospitalException(String message) {
        super(message);
    }
}
