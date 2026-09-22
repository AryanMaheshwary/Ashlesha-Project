package com.pharmacy.dao;

import com.pharmacy.model.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CustomerDao {

    public List<Customer> findAll(Connection conn) throws SQLException {
        String sql = "SELECT customer_id, name, phone, address FROM customer ORDER BY name";
        List<Customer> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public Customer findById(Connection conn, int id) throws SQLException {
        String sql = "SELECT customer_id, name, phone, address FROM customer WHERE customer_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int insert(Connection conn, Customer c) throws SQLException {
        String sql = "INSERT INTO customer (name, phone, address) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, c);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public void update(Connection conn, Customer c) throws SQLException {
        String sql = "UPDATE customer SET name = ?, phone = ?, address = ? WHERE customer_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, c);
            ps.setInt(4, c.customerId);
            ps.executeUpdate();
        }
    }

    public void delete(Connection conn, int id) throws SQLException {
        String sql = "DELETE FROM customer WHERE customer_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public long count(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM customer");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    private void bind(PreparedStatement ps, Customer c) throws SQLException {
        ps.setString(1, c.name);
        ps.setString(2, c.phone);
        ps.setString(3, c.address);
    }

    private Customer map(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.customerId = rs.getInt("customer_id");
        c.name = rs.getString("name");
        c.phone = rs.getString("phone");
        c.address = rs.getString("address");
        return c;
    }
}
