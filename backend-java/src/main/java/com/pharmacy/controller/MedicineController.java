package com.pharmacy.controller;

import com.pharmacy.model.Medicine;
import com.pharmacy.security.Auth;
import com.pharmacy.service.MedicineService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/medicines")
@Auth
public class MedicineController {

    private final MedicineService medicineService;

    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @GetMapping
    public List<Medicine> list(@RequestParam(value = "search", required = false) String search) {
        return medicineService.search(search);
    }

    @GetMapping("/{id}")
    public Medicine get(@PathVariable int id) {
        return medicineService.get(id);
    }

    @Auth(roles = {"ADMIN", "PHARMACIST"})
    @PostMapping
    public Medicine create(@RequestBody Medicine m) {
        return medicineService.create(m);
    }

    @Auth(roles = {"ADMIN", "PHARMACIST"})
    @PutMapping("/{id}")
    public Medicine update(@PathVariable int id, @RequestBody Medicine m) {
        return medicineService.update(id, m);
    }

    @Auth(roles = {"ADMIN"})
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable int id) {
        medicineService.delete(id);
        return Map.of("success", true);
    }
}
