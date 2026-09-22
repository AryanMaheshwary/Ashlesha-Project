package com.pharmacy.dao;

import com.pharmacy.model.Sale;
import com.pharmacy.model.SaleItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class SaleDao {

    public int insertSale(Connection conn, Sale s) throws SQLException {
        String sql = "INSERT INTO sale (customer_id, user_id, prescription_id, sale_date, total_amount, payment_mode) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (s.customerId == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, s.customerId);
            }
            ps.setInt(2, s.userId);
            if (s.prescriptionId == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, s.prescriptionId);
            }
            ps.setTimestamp(4, Timestamp.valueOf(s.saleDate));
            ps.setBigDecimal(5, s.totalAmount);
            ps.setString(6, s.paymentMode);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public int insertItem(Connection conn, SaleItem it) throws SQLException {
        String sql = "INSERT INTO sale_item (sale_id, medicine_id, batch_id, quantity, selling_price) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, it.saleId);
            ps.setInt(2, it.medicineId);
            ps.setInt(3, it.batchId);
            ps.setInt(4, it.quantity);
            ps.setBigDecimal(5, it.sellingPrice);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public List<Sale> findAll(Connection conn) throws SQLException {
        List<Sale> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(saleSelect() + "ORDER BY s.sale_date DESC, s.sale_id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapSale(rs));
            }
        }
        return list;
    }

    public Sale findById(Connection conn, int saleId) throws SQLException {
        Sale sale;
        try (PreparedStatement ps = conn.prepareStatement(saleSelect() + "WHERE s.sale_id = ?")) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                sale = mapSale(rs);
            }
        }
        sale.items = findItems(conn, saleId);
        return sale;
    }

    public List<SaleItem> findItems(Connection conn, int saleId) throws SQLException {
        String sql = "SELECT si.sale_item_id, si.sale_id, si.medicine_id, si.batch_id, si.quantity, si.selling_price, " +
                "m.name AS medicine_name, sb.batch_no FROM sale_item si " +
                "JOIN medicine m ON m.medicine_id = si.medicine_id " +
                "JOIN stock_batch sb ON sb.batch_id = si.batch_id WHERE si.sale_id = ? ORDER BY si.sale_item_id";
        List<SaleItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SaleItem it = new SaleItem();
                    it.saleItemId = rs.getInt("sale_item_id");
                    it.saleId = rs.getInt("sale_id");
                    it.medicineId = rs.getInt("medicine_id");
                    it.batchId = rs.getInt("batch_id");
                    it.quantity = rs.getInt("quantity");
                    it.sellingPrice = rs.getBigDecimal("selling_price");
                    it.medicineName = rs.getString("medicine_name");
                    it.batchNo = rs.getString("batch_no");
                    items.add(it);
                }
            }
        }
        return items;
    }

    private String saleSelect() {
        return "SELECT s.sale_id, s.customer_id, s.user_id, s.prescription_id, s.sale_date, s.total_amount, " +
                "s.payment_mode, c.name AS customer_name, u.full_name AS user_name FROM sale s " +
                "LEFT JOIN customer c ON c.customer_id = s.customer_id " +
                "JOIN app_user u ON u.user_id = s.user_id ";
    }

    private Sale mapSale(ResultSet rs) throws SQLException {
        Sale s = new Sale();
        s.saleId = rs.getInt("sale_id");
        int cid = rs.getInt("customer_id");
        s.customerId = rs.wasNull() ? null : cid;
        s.userId = rs.getInt("user_id");
        int pid = rs.getInt("prescription_id");
        s.prescriptionId = rs.wasNull() ? null : pid;
        s.saleDate = rs.getTimestamp("sale_date").toLocalDateTime();
        s.totalAmount = rs.getBigDecimal("total_amount");
        s.paymentMode = rs.getString("payment_mode");
        s.customerName = rs.getString("customer_name");
        s.userName = rs.getString("user_name");
        return s;
    }
}
