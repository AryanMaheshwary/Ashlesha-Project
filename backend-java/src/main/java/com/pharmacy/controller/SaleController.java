package com.pharmacy.controller;

import com.pharmacy.dto.SaleRequest;
import com.pharmacy.model.Sale;
import com.pharmacy.security.Auth;
import com.pharmacy.security.UserContext;
import com.pharmacy.service.SaleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
@Auth
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping
    public List<Sale> list() {
        return saleService.list();
    }

    @GetMapping("/{id}")
    public Sale get(@PathVariable int id) {
        return saleService.get(id);
    }

    @PostMapping
    public Sale create(@RequestBody SaleRequest req, HttpServletRequest request) {
        return saleService.createSale(UserContext.current(request).userId, req);
    }
}
