package com.pharmacy.dao;

import com.pharmacy.model.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SupplierDao {

    public List<Supplier> findAll(Connection conn) throws SQLException {
        String sql = "SELECT supplier_id, name, contact_no, email, address FROM supplier ORDER BY name";
        List<Supplier> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public Supplier findById(Connection conn, int id) throws SQLException {
        String sql = "SELECT supplier_id, name, contact_no, email, address FROM supplier WHERE supplier_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int insert(Connection conn, Supplier s) throws SQLException {
        String sql = "INSERT INTO supplier (name, contact_no, email, address) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, s);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public void update(Connection conn, Supplier s) throws SQLException {
        String sql = "UPDATE supplier SET name = ?, contact_no = ?, email = ?, address = ? WHERE supplier_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, s);
            ps.setInt(5, s.supplierId);
            ps.executeUpdate();
        }
    }

    public void delete(Connection conn, int id) throws SQLException {
        String sql = "DELETE FROM supplier WHERE supplier_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public long count(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM supplier");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    private void bind(PreparedStatement ps, Supplier s) throws SQLException {
        ps.setString(1, s.name);
        ps.setString(2, s.contactNo);
        ps.setString(3, s.email);
        ps.setString(4, s.address);
    }

    private Supplier map(ResultSet rs) throws SQLException {
        Supplier s = new Supplier();
        s.supplierId = rs.getInt("supplier_id");
        s.name = rs.getString("name");
        s.contactNo = rs.getString("contact_no");
        s.email = rs.getString("email");
        s.address = rs.getString("address");
        return s;
    }
}
