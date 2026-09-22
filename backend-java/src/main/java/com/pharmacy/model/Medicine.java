package com.pharmacy.model;

import java.math.BigDecimal;

public class Medicine {
    public Integer medicineId;
    public String name;
    public String category;
    public String manufacturer;
    public String unit;
    public BigDecimal unitPrice;
    public boolean prescriptionRequired;

    // Populated by join queries (not a physical column).
    public Integer totalAvailable;
}
