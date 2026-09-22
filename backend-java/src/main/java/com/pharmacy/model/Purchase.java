package com.pharmacy.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Purchase {
    public Integer purchaseId;
    public Integer supplierId;
    public Integer userId;
    public LocalDateTime purchaseDate;
    public BigDecimal totalAmount;

    // Populated by join queries / composition.
    public String supplierName;
    public String userName;
    public List<PurchaseItem> items;
}
