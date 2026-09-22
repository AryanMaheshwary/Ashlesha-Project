package com.pharmacy.dao;

import com.pharmacy.dto.ReportDtos.RevenuePoint;
import com.pharmacy.dto.ReportDtos.TopMedicine;
import com.pharmacy.model.Medicine;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReportDao {

    public List<Medicine> lowStock(Connection conn, int threshold) throws SQLException {
        String sql = "SELECT m.medicine_id, m.name, m.category, m.manufacturer, m.unit, m.unit_price, " +
                "m.prescription_required, " +
                "COALESCE(SUM(sb.quantity_available), 0) AS total_available FROM medicine m " +
                "LEFT JOIN stock_batch sb ON sb.medicine_id = m.medicine_id " +
                "GROUP BY m.medicine_id, m.name, m.category, m.manufacturer, m.unit, m.unit_price, m.prescription_required " +
                "HAVING total_available < ? ORDER BY total_available ASC";
        List<Medicine> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, threshold);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Medicine m = new Medicine();
                    m.medicineId = rs.getInt("medicine_id");
                    m.name = rs.getString("name");
                    m.category = rs.getString("category");
                    m.manufacturer = rs.getString("manufacturer");
                    m.unit = rs.getString("unit");
                    m.unitPrice = rs.getBigDecimal("unit_price");
                    m.prescriptionRequired = rs.getBoolean("prescription_required");
                    m.totalAvailable = rs.getInt("total_available");
                    list.add(m);
                }
            }
        }
        return list;
    }

    /** period = 'daily' groups by date; 'monthly' groups by year-month. */
    public List<RevenuePoint> revenue(Connection conn, String period) throws SQLException {
        String bucket = "monthly".equalsIgnoreCase(period)
                ? "DATE_FORMAT(sale_date, '%Y-%m')"
                : "DATE_FORMAT(sale_date, '%Y-%m-%d')";
        String sql = "SELECT " + bucket + " AS bucket, COUNT(*) AS cnt, SUM(total_amount) AS revenue " +
                "FROM sale GROUP BY bucket ORDER BY bucket DESC LIMIT 30";
        List<RevenuePoint> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                RevenuePoint p = new RevenuePoint();
                p.period = rs.getString("bucket");
                p.orderCount = rs.getInt("cnt");
                p.revenue = rs.getBigDecimal("revenue");
                list.add(p);
            }
        }
        // Return ascending for charting.
        java.util.Collections.reverse(list);
        return list;
    }

    public List<TopMedicine> topSelling(Connection conn, int limit) throws SQLException {
        String sql = "SELECT m.medicine_id, m.name, SUM(si.quantity) AS qty, " +
                "SUM(si.quantity * si.selling_price) AS revenue FROM sale_item si " +
                "JOIN medicine m ON m.medicine_id = si.medicine_id " +
                "GROUP BY m.medicine_id, m.name ORDER BY qty DESC LIMIT ?";
        List<TopMedicine> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TopMedicine t = new TopMedicine();
                    t.medicineId = rs.getInt("medicine_id");
                    t.name = rs.getString("name");
                    t.quantitySold = rs.getInt("qty");
                    t.revenue = rs.getBigDecimal("revenue");
                    list.add(t);
                }
            }
        }
        return list;
    }

    public BigDecimal revenueToday(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM sale WHERE DATE(sale_date) = CURDATE()";
        return scalarMoney(conn, sql);
    }

    public BigDecimal revenueThisMonth(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM sale " +
                "WHERE DATE_FORMAT(sale_date, '%Y-%m') = DATE_FORMAT(CURDATE(), '%Y-%m')";
        return scalarMoney(conn, sql);
    }

    public int lowStockCount(Connection conn, int threshold) throws SQLException {
        return lowStock(conn, threshold).size();
    }

    public int expiringCount(Connection conn, int days) throws SQLException {
        String sql = "SELECT COUNT(*) FROM stock_batch WHERE quantity_available > 0 " +
                "AND expiry_date >= CURDATE() AND expiry_date <= DATE_ADD(CURDATE(), INTERVAL ? DAY)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, days);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private BigDecimal scalarMoney(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        }
    }
}
