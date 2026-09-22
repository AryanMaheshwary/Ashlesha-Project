package com.pharmacy.dao;

import com.pharmacy.model.StockBatch;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class StockBatchDao {

    public List<StockBatch> findByMedicine(Connection conn, int medicineId) throws SQLException {
        String sql = "SELECT sb.batch_id, sb.medicine_id, sb.batch_no, sb.manufacture_date, sb.expiry_date, " +
                "sb.quantity_available, m.name AS medicine_name FROM stock_batch sb " +
                "JOIN medicine m ON m.medicine_id = sb.medicine_id " +
                "WHERE sb.medicine_id = ? ORDER BY sb.expiry_date ASC";
        return query(conn, sql, medicineId);
    }

    /** Non-expired batches with stock, nearest expiry first (FIFO by expiry). */
    public List<StockBatch> findSellableByMedicine(Connection conn, int medicineId) throws SQLException {
        String sql = "SELECT sb.batch_id, sb.medicine_id, sb.batch_no, sb.manufacture_date, sb.expiry_date, " +
                "sb.quantity_available, m.name AS medicine_name FROM stock_batch sb " +
                "JOIN medicine m ON m.medicine_id = sb.medicine_id " +
                "WHERE sb.medicine_id = ? AND sb.quantity_available > 0 AND sb.expiry_date >= CURDATE() " +
                "ORDER BY sb.expiry_date ASC, sb.batch_id ASC";
        return query(conn, sql, medicineId);
    }

    public int expiredStock(Connection conn, int medicineId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantity_available),0) FROM stock_batch " +
                "WHERE medicine_id = ? AND quantity_available > 0 AND expiry_date < CURDATE()";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, medicineId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<StockBatch> expiringWithin(Connection conn, int days) throws SQLException {
        String sql = "SELECT sb.batch_id, sb.medicine_id, sb.batch_no, sb.manufacture_date, sb.expiry_date, " +
                "sb.quantity_available, m.name AS medicine_name FROM stock_batch sb " +
                "JOIN medicine m ON m.medicine_id = sb.medicine_id " +
                "WHERE sb.quantity_available > 0 AND sb.expiry_date >= CURDATE() " +
                "AND sb.expiry_date <= DATE_ADD(CURDATE(), INTERVAL ? DAY) ORDER BY sb.expiry_date ASC";
        return query(conn, sql, days);
    }

    public StockBatch findById(Connection conn, int batchId) throws SQLException {
        String sql = "SELECT sb.batch_id, sb.medicine_id, sb.batch_no, sb.manufacture_date, sb.expiry_date, " +
                "sb.quantity_available, m.name AS medicine_name FROM stock_batch sb " +
                "JOIN medicine m ON m.medicine_id = sb.medicine_id WHERE sb.batch_id = ?";
        List<StockBatch> list = query(conn, sql, batchId);
        return list.isEmpty() ? null : list.get(0);
    }

    public int insert(Connection conn, StockBatch b) throws SQLException {
        String sql = "INSERT INTO stock_batch (medicine_id, batch_no, manufacture_date, expiry_date, quantity_available) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, b.medicineId);
            ps.setString(2, b.batchNo);
            ps.setDate(3, b.manufactureDate == null ? null : Date.valueOf(b.manufactureDate));
            ps.setDate(4, Date.valueOf(b.expiryDate));
            ps.setInt(5, b.quantityAvailable);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    /** Conditionally decrement stock; returns rows affected (0 if concurrent race left too little). */
    public int decrementQuantity(Connection conn, int batchId, int qty) throws SQLException {
        String sql = "UPDATE stock_batch SET quantity_available = quantity_available - ? " +
                "WHERE batch_id = ? AND quantity_available >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setInt(2, batchId);
            ps.setInt(3, qty);
            return ps.executeUpdate();
        }
    }

    private List<StockBatch> query(Connection conn, String sql, int param) throws SQLException {
        List<StockBatch> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    private StockBatch map(ResultSet rs) throws SQLException {
        StockBatch b = new StockBatch();
        b.batchId = rs.getInt("batch_id");
        b.medicineId = rs.getInt("medicine_id");
        b.batchNo = rs.getString("batch_no");
        Date md = rs.getDate("manufacture_date");
        b.manufactureDate = md == null ? null : md.toLocalDate();
        b.expiryDate = rs.getDate("expiry_date").toLocalDate();
        b.quantityAvailable = rs.getInt("quantity_available");
        b.medicineName = rs.getString("medicine_name");
        return b;
    }
}
