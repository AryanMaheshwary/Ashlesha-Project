package com.pharmacy.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Sale {
    public Integer saleId;
    public Integer customerId;
    public Integer userId;
    public Integer prescriptionId;
    public LocalDateTime saleDate;
    public BigDecimal totalAmount;
    public String paymentMode;

    // Populated by join queries / composition.
    public String customerName;
    public String userName;
    public List<SaleItem> items;
}
