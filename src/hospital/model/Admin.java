package hospital.model;

/** INHERITANCE: Admin IS-A Person. */
public class Admin extends Person {
    private static final long serialVersionUID = 1L;

    private final String password;

    public Admin(String id, String name, String phone, String email, String password) {
        super(id, name, phone, email);
        this.password = password;
    }

    public boolean authenticate(String email, String pass) {
        return getEmail().equalsIgnoreCase(email.trim()) && password.equals(pass);
    }

    @Override
    public String getRole() {
        return "Admin";
    }
}
