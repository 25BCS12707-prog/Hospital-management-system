# Hospital Management System - Java OOP Project

A console-based Hospital Management System written in **pure Java (no libraries, no database)**.
It is the Java/OOP version of the React + Firebase web app, with the same features:
patients, doctors, appointments, treatments (diagnosis + prescription) and automatic billing.

## How to run
Requires JDK 17 or newer (`javac -version` to check).

    Windows : double-click run.bat   (or run it in cmd)
    Linux/Mac: ./run.sh

Manual way:

    javac -d out -sourcepath src src/hospital/Main.java
    java -cp out hospital.Main

Data is saved to `hospital.dat` when you choose "Save & exit" and loaded on the next start.
Delete that file to reset to the sample data.

## Sample logins (sample data is created on first run)
| Role    | Login                                   |
|---------|-----------------------------------------|
| Admin   | admin@hospital.com / admin123           |
| Doctor  | D001, D002 or D003 (ID or email)        |
| Patient | P001 or P002 (ID or email)              |

## Web frontend (React + Vite) — NEW
A modern blue-and-white web dashboard on top of the same Java business logic.
The console app (`run.sh`) still works exactly as before.

**Run it (Mac / Linux) — needs JDK 17+ and Node.js 18+:**

    ./run-web.sh          # builds frontend once, compiles Java, serves everything on http://localhost:8080

**Development with hot reload:**

    ./run-dev.sh          # Java API on :8080 + Vite on http://localhost:5173

How it fits together:

    frontend/                     React + Vite app (login, role dashboards, pages)
    src/hospital/api/ApiServer.java  tiny REST/JSON layer using the JDK's built-in HTTP server (no libraries)
    src/hospital/api/Json.java       minimal JSON reader/writer
    src/hospital/service/...         UNCHANGED business logic - every button calls HospitalService

Pages: Admin (dashboard, patients, doctors, appointments, billing), Doctor (dashboard, appointments with
confirm / complete + diagnosis, my patients, treatments), Patient (dashboard, find doctor & book,
appointments, bills & pay, medical records). Data is saved to `hospital.dat` after every change.
Sample logins are the same as above (admin@hospital.com / admin123, D001, P001...).

Only change to existing logic: `HospitalService` no longer "uses up" an ID (P003, B002...) when a
registration or bill fails validation.

## Features
- **Patient**: register, book appointment (free-slot check), cancel, view treatment history, view and pay bills
- **Doctor**: register, view own appointments, confirm / cancel, complete appointment with diagnosis + prescription (bill is generated automatically from the consultation fee)
- **Admin**: dashboard (counts, revenue, pending amount), view all patients / doctors / appointments / bills, create custom bills
- **Rules**: no double booking, no booking in the past, appointment status flow `PENDING -> CONFIRMED -> COMPLETED` (or `CANCELLED`), a bill cannot be paid twice, input validation (10-digit phone, valid email, age 0-120)

## Project structure
    src/hospital/
      Main.java                    console menus (user interface)
      service/HospitalService.java business logic + file save/load
      model/
        Person.java                abstract base class
        Patient.java  Doctor.java  Admin.java      (extend Person)
        Appointment.java  Treatment.java  Bill.java
        Payable.java               interface (implemented by Bill)
        Gender.java  AppointmentStatus.java  PaymentStatus.java   (enums)
      exception/HospitalException.java   custom exception

## Class diagram
                      <<abstract>>
                        Person  ------------------ getRole() [abstract]
                       /   |   \                    getDetails() [overridden]
                 Patient Doctor Admin

     <<interface>> Payable            Appointment ---> Patient, Doctor
           ^                          Treatment   ---> Appointment
           |                          Bill        ---> Patient
          Bill

     HospitalService  (HAS-A collections of Patient, Doctor, Appointment, Treatment, Bill)

## OOP concepts used (where to find them)
| Concept | Where |
|---|---|
| **Class & Object** | Every class in `model/`; objects created in `HospitalService` |
| **Encapsulation** | Private fields + getters/setters with validation in `Person`, `Patient`, `Doctor`, `Bill`; `Patient.getMedicalHistory()` returns an unmodifiable list |
| **Abstraction** | `abstract class Person` (abstract `getRole()`), `interface Payable` |
| **Inheritance** | `Patient`, `Doctor`, `Admin` extend `Person` (use `super(...)` constructor call) |
| **Polymorphism (overriding)** | `getRole()` and `getDetails()` overridden in each subclass; `Main.printPeople()` calls `p.getDetails()` on a `Person` reference and the correct version runs |
| **Polymorphism (upcasting)** | `findAppointmentOf(Person owner, ...)` accepts a Patient, Doctor or Admin; `sum(Collection<? extends Payable>)` works on any Payable |
| **Interface** | `Payable` implemented by `Bill` |
| **Enums** | `Gender`, `AppointmentStatus`, `PaymentStatus` |
| **Association / Aggregation** | `Appointment` has a `Patient` and a `Doctor`; `Treatment` has an `Appointment`; `HospitalService` holds all collections |
| **Exception handling** | Custom checked `HospitalException`; try/catch in `Main`; `IllegalArgumentException` for invalid data |
| **Collections & Generics** | `LinkedHashMap`, `ArrayList`, `List<? extends Person>` |
| **Static / final** | `private static final` constants, `final` fields for IDs |
| **File handling / Serialization** | `HospitalService.save()` and `load()` using `ObjectOutputStream` / `ObjectInputStream` |
| **Packages** | `hospital`, `hospital.model`, `hospital.service`, `hospital.exception` |
| **Java 8+ features** | Streams / lambdas in `HospitalService` |

## Possible viva questions
1. *Why is `Person` abstract?* - We never need a "generic" person; it only holds common data and forces subclasses to define `getRole()`.
2. *Where is method overriding used?* - `getDetails()` in `Patient` and `Doctor`, `getRole()` in all three subclasses.
3. *Difference between abstract class and interface here?* - `Person` shares state (id, name, phone, email) and code; `Payable` only defines a contract that any class can promise to follow.
4. *How is encapsulation achieved?* - Fields are private; setters validate input (for example `setPhone` rejects anything that is not 10 digits).
5. *Why a custom exception?* - To report business-rule violations (slot taken, wrong status change) clearly and handle them separately from programming errors.
6. *What is Serializable used for?* - To save the entire hospital state to one file and restore it later.

## Ideas to extend (for extra marks)
- Replace the file with a MySQL database using JDBC
- Add a Swing / JavaFX GUI instead of the console
- Add `Nurse` or `Receptionist` classes extending `Person`
- Add discount rules with an interface such as `Discountable`
