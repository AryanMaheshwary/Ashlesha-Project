package com.pharmacy.dao;

import com.pharmacy.model.Medicine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MedicineDao {

    private static final String BASE =
            "SELECT m.medicine_id, m.name, m.category, m.manufacturer, m.unit, m.unit_price, m.prescription_required, " +
            "COALESCE((SELECT SUM(sb.quantity_available) FROM stock_batch sb WHERE sb.medicine_id = m.medicine_id), 0) AS total_available " +
            "FROM medicine m ";

    public List<Medicine> search(Connection conn, String term) throws SQLException {
        String sql = BASE + (term == null || term.isBlank()
                ? "ORDER BY m.name"
                : "WHERE m.name LIKE ? OR m.category LIKE ? OR m.manufacturer LIKE ? ORDER BY m.name");
        List<Medicine> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (term != null && !term.isBlank()) {
                String like = "%" + term.trim() + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public Medicine findById(Connection conn, int id) throws SQLException {
        String sql = BASE + "WHERE m.medicine_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int insert(Connection conn, Medicine m) throws SQLException {
        String sql = "INSERT INTO medicine (name, category, manufacturer, unit, unit_price, prescription_required) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, m);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public void update(Connection conn, Medicine m) throws SQLException {
        String sql = "UPDATE medicine SET name = ?, category = ?, manufacturer = ?, unit = ?, unit_price = ?, " +
                "prescription_required = ? WHERE medicine_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, m);
            ps.setInt(7, m.medicineId);
            ps.executeUpdate();
        }
    }

    public void delete(Connection conn, int id) throws SQLException {
        String sql = "DELETE FROM medicine WHERE medicine_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public long count(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM medicine");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    private void bind(PreparedStatement ps, Medicine m) throws SQLException {
        ps.setString(1, m.name);
        ps.setString(2, m.category);
        ps.setString(3, m.manufacturer);
        ps.setString(4, m.unit);
        ps.setBigDecimal(5, m.unitPrice);
        ps.setBoolean(6, m.prescriptionRequired);
    }

    private Medicine map(ResultSet rs) throws SQLException {
        Medicine m = new Medicine();
        m.medicineId = rs.getInt("medicine_id");
        m.name = rs.getString("name");
        m.category = rs.getString("category");
        m.manufacturer = rs.getString("manufacturer");
        m.unit = rs.getString("unit");
        m.unitPrice = rs.getBigDecimal("unit_price");
        m.prescriptionRequired = rs.getBoolean("prescription_required");
        m.totalAvailable = rs.getInt("total_available");
        return m;
    }
}
