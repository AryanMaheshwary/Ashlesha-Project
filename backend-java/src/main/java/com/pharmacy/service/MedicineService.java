package com.pharmacy.service;

import com.pharmacy.dao.MedicineDao;
import com.pharmacy.db.ConnectionManager;
import com.pharmacy.exception.NotFoundException;
import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.Medicine;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Service
public class MedicineService {

    private final MedicineDao medicineDao = new MedicineDao();

    public List<Medicine> search(String term) {
        try (Connection conn = ConnectionManager.getConnection()) {
            return medicineDao.search(conn, term);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Medicine get(int id) {
        try (Connection conn = ConnectionManager.getConnection()) {
            Medicine m = medicineDao.findById(conn, id);
            if (m == null) {
                throw new NotFoundException("Medicine not found: " + id);
            }
            return m;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Medicine create(Medicine m) {
        validate(m);
        try (Connection conn = ConnectionManager.getConnection()) {
            int id = medicineDao.insert(conn, m);
            return medicineDao.findById(conn, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Medicine update(int id, Medicine m) {
        validate(m);
        try (Connection conn = ConnectionManager.getConnection()) {
            if (medicineDao.findById(conn, id) == null) {
                throw new NotFoundException("Medicine not found: " + id);
            }
            m.medicineId = id;
            medicineDao.update(conn, m);
            return medicineDao.findById(conn, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void delete(int id) {
        try (Connection conn = ConnectionManager.getConnection()) {
            medicineDao.delete(conn, id);
        } catch (SQLException e) {
            throw new ValidationException("Cannot delete medicine: it is referenced by stock or sales records.");
        }
    }

    private void validate(Medicine m) {
        if (m.name == null || m.name.isBlank()) {
            throw new ValidationException("Medicine name is required.");
        }
        if (m.category == null || m.category.isBlank()) {
            throw new ValidationException("Category is required.");
        }
        if (m.unit == null || m.unit.isBlank()) {
            throw new ValidationException("Unit is required.");
        }
        if (m.unitPrice == null || m.unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Unit price must be zero or greater.");
        }
    }
}
