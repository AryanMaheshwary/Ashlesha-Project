package com.pharmacy.service;

import com.pharmacy.dao.ReportDao;
import com.pharmacy.dao.StockBatchDao;
import com.pharmacy.db.ConnectionManager;
import com.pharmacy.dto.ReportDtos.RevenuePoint;
import com.pharmacy.dto.ReportDtos.Summary;
import com.pharmacy.dto.ReportDtos.TopMedicine;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.StockBatch;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Service
public class ReportService {

    public static final int DEFAULT_LOW_STOCK_THRESHOLD = 20;
    public static final int EXPIRY_WINDOW_DAYS = 30;

    private final ReportDao reportDao = new ReportDao();
    private final StockBatchDao batchDao = new StockBatchDao();

    public List<Medicine> lowStock(int threshold) {
        try (Connection conn = ConnectionManager.getConnection()) {
            return reportDao.lowStock(conn, threshold);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public List<StockBatch> expiring(int days) {
        try (Connection conn = ConnectionManager.getConnection()) {
            return batchDao.expiringWithin(conn, days);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public List<RevenuePoint> revenue(String period) {
        try (Connection conn = ConnectionManager.getConnection()) {
            return reportDao.revenue(conn, period);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public List<TopMedicine> topSelling(int limit) {
        try (Connection conn = ConnectionManager.getConnection()) {
            return reportDao.topSelling(conn, limit);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Summary summary() {
        try (Connection conn = ConnectionManager.getConnection()) {
            Summary s = new Summary();
            s.dailyRevenue = reportDao.revenueToday(conn);
            s.monthlyRevenue = reportDao.revenueThisMonth(conn);
            s.lowStockCount = reportDao.lowStockCount(conn, DEFAULT_LOW_STOCK_THRESHOLD);
            s.expiringCount = reportDao.expiringCount(conn, EXPIRY_WINDOW_DAYS);
            return s;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
