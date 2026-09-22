package com.pharmacy.dto;

import java.math.BigDecimal;

public class ReportDtos {

    public static class RevenuePoint {
        public String period;
        public int orderCount;
        public BigDecimal revenue;
    }

    public static class TopMedicine {
        public Integer medicineId;
        public String name;
        public int quantitySold;
        public BigDecimal revenue;
    }

    public static class Summary {
        public BigDecimal dailyRevenue;
        public BigDecimal monthlyRevenue;
        public int lowStockCount;
        public int expiringCount;
    }
}
