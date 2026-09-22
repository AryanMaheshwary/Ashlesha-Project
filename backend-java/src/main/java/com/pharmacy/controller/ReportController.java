package com.pharmacy.controller;

import com.pharmacy.dto.ReportDtos.RevenuePoint;
import com.pharmacy.dto.ReportDtos.Summary;
import com.pharmacy.dto.ReportDtos.TopMedicine;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.StockBatch;
import com.pharmacy.security.Auth;
import com.pharmacy.service.ReportService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@Auth(roles = {"ADMIN", "PHARMACIST"})
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    public Summary summary() {
        return reportService.summary();
    }

    @GetMapping("/low-stock")
    public List<Medicine> lowStock(@RequestParam(value = "threshold", defaultValue = "20") int threshold) {
        return reportService.lowStock(threshold);
    }

    @GetMapping("/expiry")
    public List<StockBatch> expiry(@RequestParam(value = "days", defaultValue = "30") int days) {
        return reportService.expiring(days);
    }

    @GetMapping("/revenue")
    public List<RevenuePoint> revenue(@RequestParam(value = "period", defaultValue = "daily") String period) {
        return reportService.revenue(period);
    }

    @GetMapping("/top-selling")
    public List<TopMedicine> topSelling(@RequestParam(value = "limit", defaultValue = "10") int limit) {
        return reportService.topSelling(limit);
    }
}
