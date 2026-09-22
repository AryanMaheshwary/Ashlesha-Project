package com.pharmacy.service;

import com.pharmacy.dao.CustomerDao;
import com.pharmacy.db.ConnectionManager;
import com.pharmacy.exception.NotFoundException;
import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.Customer;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Service
public class CustomerService {

    private final CustomerDao customerDao = new CustomerDao();

    public List<Customer> list() {
        try (Connection conn = ConnectionManager.getConnection()) {
            return customerDao.findAll(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Customer create(Customer c) {
        validate(c);
        try (Connection conn = ConnectionManager.getConnection()) {
            int id = customerDao.insert(conn, c);
            return customerDao.findById(conn, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Customer update(int id, Customer c) {
        validate(c);
        try (Connection conn = ConnectionManager.getConnection()) {
            if (customerDao.findById(conn, id) == null) {
                throw new NotFoundException("Customer not found: " + id);
            }
            c.customerId = id;
            customerDao.update(conn, c);
            return customerDao.findById(conn, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void delete(int id) {
        try (Connection conn = ConnectionManager.getConnection()) {
            customerDao.delete(conn, id);
        } catch (SQLException e) {
            throw new ValidationException("Cannot delete customer: it is referenced by sales or prescriptions.");
        }
    }

    private void validate(Customer c) {
        if (c.name == null || c.name.isBlank()) {
            throw new ValidationException("Customer name is required.");
        }
    }
}
