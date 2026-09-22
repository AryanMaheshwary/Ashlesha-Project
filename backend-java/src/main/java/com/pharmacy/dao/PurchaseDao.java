package com.pharmacy.dao;

import com.pharmacy.model.Purchase;
import com.pharmacy.model.PurchaseItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PurchaseDao {

    public int insertPurchase(Connection conn, Purchase p) throws SQLException {
        String sql = "INSERT INTO purchase (supplier_id, user_id, purchase_date, total_amount) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.supplierId);
            ps.setInt(2, p.userId);
            ps.setTimestamp(3, Timestamp.valueOf(p.purchaseDate));
            ps.setBigDecimal(4, p.totalAmount);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public int insertItem(Connection conn, PurchaseItem it) throws SQLException {
        String sql = "INSERT INTO purchase_item (purchase_id, medicine_id, batch_id, quantity, cost_price) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, it.purchaseId);
            ps.setInt(2, it.medicineId);
            ps.setInt(3, it.batchId);
            ps.setInt(4, it.quantity);
            ps.setBigDecimal(5, it.costPrice);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public List<Purchase> findAll(Connection conn) throws SQLException {
        String sql = "SELECT p.purchase_id, p.supplier_id, p.user_id, p.purchase_date, p.total_amount, " +
                "s.name AS supplier_name, u.full_name AS user_name FROM purchase p " +
                "JOIN supplier s ON s.supplier_id = p.supplier_id " +
                "JOIN app_user u ON u.user_id = p.user_id ORDER BY p.purchase_date DESC, p.purchase_id DESC";
        List<Purchase> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Purchase p = new Purchase();
                p.purchaseId = rs.getInt("purchase_id");
                p.supplierId = rs.getInt("supplier_id");
                p.userId = rs.getInt("user_id");
                p.purchaseDate = rs.getTimestamp("purchase_date").toLocalDateTime();
                p.totalAmount = rs.getBigDecimal("total_amount");
                p.supplierName = rs.getString("supplier_name");
                p.userName = rs.getString("user_name");
                list.add(p);
            }
        }
        return list;
    }
}
