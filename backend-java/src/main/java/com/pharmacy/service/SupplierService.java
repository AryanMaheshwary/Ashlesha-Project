package com.pharmacy.service;

import com.pharmacy.dao.SupplierDao;
import com.pharmacy.db.ConnectionManager;
import com.pharmacy.exception.NotFoundException;
import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.Supplier;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Service
public class SupplierService {

    private final SupplierDao supplierDao = new SupplierDao();

    public List<Supplier> list() {
        try (Connection conn = ConnectionManager.getConnection()) {
            return supplierDao.findAll(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Supplier create(Supplier s) {
        validate(s);
        try (Connection conn = ConnectionManager.getConnection()) {
            int id = supplierDao.insert(conn, s);
            return supplierDao.findById(conn, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Supplier update(int id, Supplier s) {
        validate(s);
        try (Connection conn = ConnectionManager.getConnection()) {
            if (supplierDao.findById(conn, id) == null) {
                throw new NotFoundException("Supplier not found: " + id);
            }
            s.supplierId = id;
            supplierDao.update(conn, s);
            return supplierDao.findById(conn, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void delete(int id) {
        try (Connection conn = ConnectionManager.getConnection()) {
            supplierDao.delete(conn, id);
        } catch (SQLException e) {
            throw new ValidationException("Cannot delete supplier: it is referenced by purchase records.");
        }
    }

    private void validate(Supplier s) {
        if (s.name == null || s.name.isBlank()) {
            throw new ValidationException("Supplier name is required.");
        }
    }
}
