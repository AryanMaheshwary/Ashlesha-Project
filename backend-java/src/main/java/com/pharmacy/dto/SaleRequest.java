package com.pharmacy.dto;

import java.util.List;

public class SaleRequest {
    public Integer customerId;
    public Integer prescriptionId;
    public String paymentMode;
    public List<Item> items;

    public static class Item {
        public Integer medicineId;
        public Integer quantity;
    }
}
