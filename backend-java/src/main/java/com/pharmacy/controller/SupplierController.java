package com.pharmacy.controller;

import com.pharmacy.model.Supplier;
import com.pharmacy.security.Auth;
import com.pharmacy.service.SupplierService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/suppliers")
@Auth
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public List<Supplier> list() {
        return supplierService.list();
    }

    @Auth(roles = {"ADMIN", "PHARMACIST"})
    @PostMapping
    public Supplier create(@RequestBody Supplier s) {
        return supplierService.create(s);
    }

    @Auth(roles = {"ADMIN", "PHARMACIST"})
    @PutMapping("/{id}")
    public Supplier update(@PathVariable int id, @RequestBody Supplier s) {
        return supplierService.update(id, s);
    }

    @Auth(roles = {"ADMIN"})
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable int id) {
        supplierService.delete(id);
        return Map.of("success", true);
    }
}
