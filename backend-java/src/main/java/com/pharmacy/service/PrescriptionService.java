package com.pharmacy.service;

import com.pharmacy.dao.CustomerDao;
import com.pharmacy.dao.PrescriptionDao;
import com.pharmacy.db.ConnectionManager;
import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.Prescription;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Service
public class PrescriptionService {

    private final PrescriptionDao prescriptionDao = new PrescriptionDao();
    private final CustomerDao customerDao = new CustomerDao();

    public List<Prescription> list() {
        try (Connection conn = ConnectionManager.getConnection()) {
            return prescriptionDao.findAll(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Prescription create(Prescription p) {
        if (p.customerId == null) {
            throw new ValidationException("A customer is required for a prescription.");
        }
        if (p.doctorName == null || p.doctorName.isBlank()) {
            throw new ValidationException("Doctor name is required.");
        }
        if (p.issueDate == null) {
            p.issueDate = LocalDate.now();
        }
        try (Connection conn = ConnectionManager.getConnection()) {
            if (customerDao.findById(conn, p.customerId) == null) {
                throw new ValidationException("Customer not found: " + p.customerId);
            }
            int id = prescriptionDao.insert(conn, p);
            return prescriptionDao.findById(conn, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
