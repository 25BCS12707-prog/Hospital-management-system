package hospital.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** INHERITANCE: Patient IS-A Person. */
public class Patient extends Person {
    private static final long serialVersionUID = 1L;

    private int age;
    private Gender gender;
    private final List<String> medicalHistory = new ArrayList<>();

    public Patient(String id, String name, int age, Gender gender, String phone, String email) {
        super(id, name, phone, email);
        setAge(age);
        this.gender = gender;
    }

    public int getAge() { return age; }
    public Gender getGender() { return gender; }

    public void setAge(int age) {
        if (age < 0 || age > 120) {
            throw new IllegalArgumentException("Age must be between 0 and 120.");
        }
        this.age = age;
    }

    public void addMedicalRecord(String record) {
        medicalHistory.add(record);
    }

    /** Returns a read-only view so outside code cannot modify the list directly. */
    public List<String> getMedicalHistory() {
        return Collections.unmodifiableList(medicalHistory);
    }

    @Override
    public String getRole() {
        return "Patient";
    }

    @Override
    public String getDetails() {
        return super.getDetails() + String.format(" | Age: %d | %s", age, gender);
    }
}
