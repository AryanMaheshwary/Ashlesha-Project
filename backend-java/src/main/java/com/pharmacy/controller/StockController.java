package com.pharmacy.controller;

import com.pharmacy.dto.PurchaseRequest;
import com.pharmacy.model.Purchase;
import com.pharmacy.model.StockBatch;
import com.pharmacy.security.Auth;
import com.pharmacy.security.UserContext;
import com.pharmacy.service.StockService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Auth
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/batches")
    public List<StockBatch> batches(@RequestParam("medicineId") int medicineId) {
        return stockService.batchesForMedicine(medicineId);
    }

    @Auth(roles = {"ADMIN", "PHARMACIST"})
    @GetMapping("/purchases")
    public List<Purchase> purchases() {
        return stockService.purchases();
    }

    @Auth(roles = {"ADMIN", "PHARMACIST"})
    @PostMapping("/purchases")
    public Purchase recordPurchase(@RequestBody PurchaseRequest req, HttpServletRequest request) {
        return stockService.recordPurchase(UserContext.current(request).userId, req);
    }
}
