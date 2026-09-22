package com.pharmacy.model;

import java.time.LocalDate;

public class StockBatch {
    public Integer batchId;
    public Integer medicineId;
    public String batchNo;
    public LocalDate manufactureDate;
    public LocalDate expiryDate;
    public int quantityAvailable;

    // Populated by join queries.
    public String medicineName;
}
