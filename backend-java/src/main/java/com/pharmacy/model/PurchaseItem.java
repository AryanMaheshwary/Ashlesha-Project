package com.pharmacy.model;

import java.math.BigDecimal;

public class PurchaseItem {
    public Integer purchaseItemId;
    public Integer purchaseId;
    public Integer medicineId;
    public Integer batchId;
    public int quantity;
    public BigDecimal costPrice;

    public String medicineName;
    public String batchNo;
}
