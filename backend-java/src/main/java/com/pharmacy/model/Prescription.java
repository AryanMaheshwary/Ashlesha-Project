package com.pharmacy.model;

import java.time.LocalDate;

public class Prescription {
    public Integer prescriptionId;
    public Integer customerId;
    public String doctorName;
    public LocalDate issueDate;
    public String fileReference;

    // Populated by join queries.
    public String customerName;
}
