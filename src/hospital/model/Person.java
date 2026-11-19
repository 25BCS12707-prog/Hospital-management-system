package hospital.model;

import java.io.Serializable;

/**
 * ABSTRACTION + ENCAPSULATION:
 * Person is an abstract base class. It cannot be instantiated directly.
 * All fields are private and are accessed only through getters/setters,
 * where the setters validate the data.
 */
public abstract class Person implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private String name;
    private String phone;
    private String email;

    protected Person(String id, String name, String phone, String email) {
        this.id = id;
        setName(name);
        setPhone(phone);
        setEmail(email);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        this.name = name.trim();
    }

    public void setPhone(String phone) {
        if (phone == null || !phone.matches("\\d{10}")) {
            throw new IllegalArgumentException("Phone must be exactly 10 digits.");
        }
        this.phone = phone;
    }

    public void setEmail(String email) {
        if (email == null || !email.trim().matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
            throw new IllegalArgumentException("Invalid email address.");
        }
        this.email = email.trim().toLowerCase();
    }

    /** Abstract method: every subclass MUST define its own role name. */
    public abstract String getRole();

    /** Overridden by subclasses (POLYMORPHISM) to add their own details. */
    public String getDetails() {
        return String.format("%-5s | %-20s | %-10s | %s", id, name, phone, email);
    }

    @Override
    public String toString() {
        return getRole() + ": " + name + " (" + id + ")";
    }
}
