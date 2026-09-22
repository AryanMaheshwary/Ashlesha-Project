package com.pharmacy.controller;

import com.pharmacy.model.Customer;
import com.pharmacy.security.Auth;
import com.pharmacy.service.CustomerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@Auth
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public List<Customer> list() {
        return customerService.list();
    }

    @PostMapping
    public Customer create(@RequestBody Customer c) {
        return customerService.create(c);
    }

    @PutMapping("/{id}")
    public Customer update(@PathVariable int id, @RequestBody Customer c) {
        return customerService.update(id, c);
    }

    @Auth(roles = {"ADMIN", "PHARMACIST"})
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable int id) {
        customerService.delete(id);
        return Map.of("success", true);
    }
}
