package com.pharmacy.dao;

import com.pharmacy.model.Prescription;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PrescriptionDao {

    private static final String BASE =
            "SELECT p.prescription_id, p.customer_id, p.doctor_name, p.issue_date, p.file_reference, " +
            "c.name AS customer_name FROM prescription p JOIN customer c ON c.customer_id = p.customer_id ";

    public List<Prescription> findAll(Connection conn) throws SQLException {
        String sql = BASE + "ORDER BY p.issue_date DESC, p.prescription_id DESC";
        List<Prescription> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public Prescription findById(Connection conn, int id) throws SQLException {
        String sql = BASE + "WHERE p.prescription_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int insert(Connection conn, Prescription p) throws SQLException {
        String sql = "INSERT INTO prescription (customer_id, doctor_name, issue_date, file_reference) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.customerId);
            ps.setString(2, p.doctorName);
            ps.setDate(3, Date.valueOf(p.issueDate));
            ps.setString(4, p.fileReference);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    private Prescription map(ResultSet rs) throws SQLException {
        Prescription p = new Prescription();
        p.prescriptionId = rs.getInt("prescription_id");
        p.customerId = rs.getInt("customer_id");
        p.doctorName = rs.getString("doctor_name");
        p.issueDate = rs.getDate("issue_date").toLocalDate();
        p.fileReference = rs.getString("file_reference");
        p.customerName = rs.getString("customer_name");
        return p;
    }
}
