package com.pharmacy.controller;

import com.pharmacy.model.Prescription;
import com.pharmacy.security.Auth;
import com.pharmacy.service.PrescriptionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
@Auth
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @GetMapping
    public List<Prescription> list() {
        return prescriptionService.list();
    }

    @PostMapping
    public Prescription create(@RequestBody Prescription p) {
        return prescriptionService.create(p);
    }
}
