package com.pharmacy.dto;

import java.math.BigDecimal;
import java.util.List;

public class PurchaseRequest {
    public Integer supplierId;
    public List<Item> items;

    public static class Item {
        public Integer medicineId;
        public String batchNo;
        public String manufactureDate; // ISO yyyy-MM-dd, optional
        public String expiryDate;      // ISO yyyy-MM-dd, required
        public Integer quantity;
        public BigDecimal costPrice;
    }
}
