package hospital;

import hospital.exception.HospitalException;
import hospital.model.*;
import hospital.service.HospitalService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * Console user interface of the Hospital Management System.
 * Three roles, just like the original web app: Admin, Doctor, Patient.
 */
public class Main {
    private static final String DATA_FILE = "hospital.dat";
    private static final Scanner in = new Scanner(System.in);
    private static HospitalService service;

    public static void main(String[] args) {
        service = HospitalService.load(DATA_FILE);
        System.out.println("=====================================");
        System.out.println("   HOSPITAL MANAGEMENT SYSTEM (Java)");
        System.out.println("=====================================");

        boolean running = true;
        while (running) {
            System.out.println("\n--- MAIN MENU ---");
            System.out.println("1. Admin login");
            System.out.println("2. Doctor login");
            System.out.println("3. Patient login");
            System.out.println("4. Register as new patient");
            System.out.println("5. Register as new doctor");
            System.out.println("0. Save & exit");
            int choice = readInt("Choice: ");
            try {
                switch (choice) {
                    case 1 -> adminMenu(service.loginAdmin(readLine("Admin email: "), readLine("Password: ")));
                    case 2 -> doctorMenu(service.findDoctor(readLine("Doctor ID or email: ")));
                    case 3 -> patientMenu(service.findPatient(readLine("Patient ID or email: ")));
                    case 4 -> registerPatient();
                    case 5 -> registerDoctor();
                    case 0 -> running = false;
                    default -> System.out.println("Invalid choice.");
                }
            } catch (HospitalException | IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        try {
            service.save(DATA_FILE);
            System.out.println("Data saved to " + DATA_FILE + ". Goodbye!");
        } catch (HospitalException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // ================================================================= registration
    private static void registerPatient() throws HospitalException {
        String name = readLine("Name: ");
        int age = readInt("Age: ");
        Gender gender = readGender();
        String phone = readLine("Phone (10 digits): ");
        String email = readLine("Email: ");
        Patient p = service.registerPatient(name, age, gender, phone, email);
        System.out.println("Registered! Your Patient ID is " + p.getId());
    }

    private static void registerDoctor() throws HospitalException {
        String name = readLine("Name: ");
        String spec = readLine("Specialization: ");
        double fee = readDouble("Consultation fee (Rs.): ");
        String phone = readLine("Phone (10 digits): ");
        String email = readLine("Email: ");
        Doctor d = service.registerDoctor(name, spec, fee, phone, email);
        System.out.println("Registered! Your Doctor ID is " + d.getId());
    }

    // ================================================================= ADMIN
    private static void adminMenu(Admin admin) {
        System.out.println("\nWelcome, " + admin.getName());
        boolean back = false;
        while (!back) {
            System.out.println("\n--- ADMIN MENU ---");
            System.out.println("1. Dashboard");
            System.out.println("2. View all patients");
            System.out.println("3. View all doctors");
            System.out.println("4. View all appointments");
            System.out.println("5. View all bills");
            System.out.println("6. Create a bill for a patient");
            System.out.println("0. Logout");
            try {
                switch (readInt("Choice: ")) {
                    case 1 -> showDashboard();
                    case 2 -> printPeople(service.getAllPatients());
                    case 3 -> printPeople(service.getAllDoctors());
                    case 4 -> printList(service.getAllAppointments(), "No appointments yet.");
                    case 5 -> printList(service.getAllBills(), "No bills yet.");
                    case 6 -> {
                        Patient p = service.findPatient(readLine("Patient ID or email: "));
                        double amount = readDouble("Amount (Rs.): ");
                        Bill b = service.addBill(p, amount, readLine("Description: "));
                        System.out.println("Bill created: " + b);
                    }
                    case 0 -> back = true;
                    default -> System.out.println("Invalid choice.");
                }
            } catch (HospitalException | IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void showDashboard() {
        System.out.println("\n===== DASHBOARD =====");
        System.out.println("Patients           : " + service.getAllPatients().size());
        System.out.println("Doctors            : " + service.getAllDoctors().size());
        for (AppointmentStatus s : AppointmentStatus.values()) {
            System.out.printf("Appointments %-10s: %d%n", s, service.countByStatus(s));
        }
        System.out.printf("Revenue (paid)     : Rs.%.2f%n", service.getRevenue());
        System.out.printf("Pending (unpaid)   : Rs.%.2f%n", service.getPendingAmount());
    }

    // ================================================================= DOCTOR
    private static void doctorMenu(Doctor doctor) {
        System.out.println("\nWelcome, Dr. " + doctor.getName() + " (" + doctor.getSpecialization() + ")");
        boolean back = false;
        while (!back) {
            System.out.println("\n--- DOCTOR MENU ---");
            System.out.println("1. View my appointments");
            System.out.println("2. Confirm an appointment");
            System.out.println("3. Cancel an appointment");
            System.out.println("4. Complete appointment (add diagnosis + prescription, auto-bill)");
            System.out.println("0. Logout");
            try {
                switch (readInt("Choice: ")) {
                    case 1 -> printList(service.getAppointmentsOf(doctor), "No appointments yet.");
                    case 2 -> {
                        service.findAppointmentOf(doctor, readLine("Appointment ID: ")).confirm();
                        System.out.println("Appointment confirmed.");
                    }
                    case 3 -> {
                        service.findAppointmentOf(doctor, readLine("Appointment ID: ")).cancel();
                        System.out.println("Appointment cancelled.");
                    }
                    case 4 -> {
                        Appointment a = service.findAppointmentOf(doctor, readLine("Appointment ID: "));
                        String diagnosis = readLine("Diagnosis: ");
                        String prescription = readLine("Prescription: ");
                        Bill bill = service.completeAppointment(a, diagnosis, prescription);
                        System.out.println("Treatment saved. Bill generated: " + bill);
                    }
                    case 0 -> back = true;
                    default -> System.out.println("Invalid choice.");
                }
            } catch (HospitalException | IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // ================================================================= PATIENT
    private static void patientMenu(Patient patient) {
        System.out.println("\nWelcome, " + patient.getName() + " (" + patient.getId() + ")");
        boolean back = false;
        while (!back) {
            System.out.println("\n--- PATIENT MENU ---");
            System.out.println("1. Book an appointment");
            System.out.println("2. My appointments");
            System.out.println("3. Cancel an appointment");
            System.out.println("4. My treatment history");
            System.out.println("5. My bills");
            System.out.println("6. Pay a bill");
            System.out.println("0. Logout");
            try {
                switch (readInt("Choice: ")) {
                    case 1 -> bookAppointment(patient);
                    case 2 -> printList(service.getAppointmentsOf(patient), "No appointments yet.");
                    case 3 -> {
                        service.findAppointmentOf(patient, readLine("Appointment ID: ")).cancel();
                        System.out.println("Appointment cancelled.");
                    }
                    case 4 -> {
                        List<Treatment> history = service.getTreatmentsOf(patient);
                        printList(history, "No treatments yet.");
                        System.out.println("Medical history: " + patient.getMedicalHistory());
                    }
                    case 5 -> printList(service.getBillsOf(patient), "No bills.");
                    case 6 -> {
                        Bill b = service.payBill(patient, readLine("Bill ID: "));
                        System.out.printf("Paid Rs.%.2f. Thank you!%n", b.getAmount());
                    }
                    case 0 -> back = true;
                    default -> System.out.println("Invalid choice.");
                }
            } catch (HospitalException | IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void bookAppointment(Patient patient) throws HospitalException {
        printPeople(service.getAllDoctors());
        Doctor doctor = service.findDoctor(readLine("Choose doctor ID: "));
        LocalDate date = readDate("Date (yyyy-MM-dd): ");
        List<String> free = service.getFreeSlots(doctor, date);
        if (free.isEmpty()) {
            throw new HospitalException("No free slots for Dr. " + doctor.getName() + " on " + date + ".");
        }
        System.out.println("Free slots: " + free);
        String time = readLine("Choose slot (e.g. 10:00): ");
        Appointment a = service.bookAppointment(patient, doctor, date, time);
        System.out.println("Booked (status PENDING): " + a);
    }

    // ================================================================= helpers
    private static void printPeople(java.util.Collection<? extends Person> people) {
        if (people.isEmpty()) {
            System.out.println("Nothing to show.");
        }
        // POLYMORPHISM: the right getDetails() runs for Patient / Doctor automatically.
        for (Person p : people) {
            System.out.println(p.getDetails());
        }
    }

    private static void printList(List<?> items, String emptyMessage) {
        if (items.isEmpty()) {
            System.out.println(emptyMessage);
        }
        items.forEach(System.out::println);
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return in.hasNextLine() ? in.nextLine().trim() : "";
    }

    private static int readInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(readLine(prompt));
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            try {
                return Double.parseDouble(readLine(prompt));
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static LocalDate readDate(String prompt) {
        while (true) {
            try {
                return LocalDate.parse(readLine(prompt));
            } catch (DateTimeParseException e) {
                System.out.println("Use the format yyyy-MM-dd (example 2026-12-01).");
            }
        }
    }

    private static Gender readGender() {
        while (true) {
            String g = readLine("Gender (MALE / FEMALE / OTHER): ").toUpperCase();
            try {
                return Gender.valueOf(g);
            } catch (IllegalArgumentException e) {
                System.out.println("Please type MALE, FEMALE or OTHER.");
            }
        }
    }
}
