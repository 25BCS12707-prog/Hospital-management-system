package hospital.model;

import hospital.exception.HospitalException;

/**
 * INTERFACE: any class that represents money to be paid must implement this.
 * (Bill implements it. A future class like LabTest could too.)
 */
public interface Payable {
    double getAmount();

    boolean isPaid();

    void pay() throws HospitalException;
}
