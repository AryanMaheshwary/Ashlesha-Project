package com.pharmacy.model;

import java.math.BigDecimal;

public class SaleItem {
    public Integer saleItemId;
    public Integer saleId;
    public Integer medicineId;
    public Integer batchId;
    public int quantity;
    public BigDecimal sellingPrice;

    public String medicineName;
    public String batchNo;
}
