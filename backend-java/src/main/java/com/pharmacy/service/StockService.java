package com.pharmacy.service;

import com.pharmacy.dao.MedicineDao;
import com.pharmacy.dao.PurchaseDao;
import com.pharmacy.dao.StockBatchDao;
import com.pharmacy.db.ConnectionManager;
import com.pharmacy.dto.PurchaseRequest;
import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.Purchase;
import com.pharmacy.model.PurchaseItem;
import com.pharmacy.model.StockBatch;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class StockService {

    private final StockBatchDao batchDao = new StockBatchDao();
    private final MedicineDao medicineDao = new MedicineDao();
    private final PurchaseDao purchaseDao = new PurchaseDao();

    public List<StockBatch> batchesForMedicine(int medicineId) {
        try (Connection conn = ConnectionManager.getConnection()) {
            return batchDao.findByMedicine(conn, medicineId);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public List<Purchase> purchases() {
        try (Connection conn = ConnectionManager.getConnection()) {
            return purchaseDao.findAll(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Records an incoming purchase. Each line creates a NEW stock_batch (with its own
     * expiry date) plus a purchase_item. Entire operation runs in one transaction.
     */
    public Purchase recordPurchase(int userId, PurchaseRequest req) {
        if (req.supplierId == null) {
            throw new ValidationException("Supplier is required.");
        }
        if (req.items == null || req.items.isEmpty()) {
            throw new ValidationException("At least one purchase line is required.");
        }

        Connection conn = null;
        try {
            conn = ConnectionManager.getConnection();
            conn.setAutoCommit(false);

            BigDecimal total = BigDecimal.ZERO;
            for (PurchaseRequest.Item item : req.items) {
                validateItem(conn, item);
                total = total.add(item.costPrice.multiply(BigDecimal.valueOf(item.quantity)));
            }

            Purchase purchase = new Purchase();
            purchase.supplierId = req.supplierId;
            purchase.userId = userId;
            purchase.purchaseDate = LocalDateTime.now();
            purchase.totalAmount = total;
            int purchaseId = purchaseDao.insertPurchase(conn, purchase);
            purchase.purchaseId = purchaseId;

            for (PurchaseRequest.Item item : req.items) {
                StockBatch batch = new StockBatch();
                batch.medicineId = item.medicineId;
                batch.batchNo = item.batchNo;
                batch.manufactureDate = parseDate(item.manufactureDate, false);
                batch.expiryDate = parseDate(item.expiryDate, true);
                batch.quantityAvailable = item.quantity;
                int batchId = batchDao.insert(conn, batch);

                PurchaseItem pi = new PurchaseItem();
                pi.purchaseId = purchaseId;
                pi.medicineId = item.medicineId;
                pi.batchId = batchId;
                pi.quantity = item.quantity;
                pi.costPrice = item.costPrice;
                purchaseDao.insertItem(conn, pi);
            }

            conn.commit();
            return purchase;
        } catch (RuntimeException | SQLException e) {
            rollback(conn);
            if (e instanceof ValidationException ve) {
                throw ve;
            }
            throw new RuntimeException("Purchase failed: " + e.getMessage(), e);
        } finally {
            close(conn);
        }
    }

    private void validateItem(Connection conn, PurchaseRequest.Item item) throws SQLException {
        if (item.medicineId == null || medicineDao.findById(conn, item.medicineId) == null) {
            throw new ValidationException("Medicine not found for purchase line.");
        }
        if (item.batchNo == null || item.batchNo.isBlank()) {
            throw new ValidationException("Batch number is required for every purchase line.");
        }
        if (item.quantity == null || item.quantity <= 0) {
            throw new ValidationException("Quantity must be greater than zero.");
        }
        if (item.costPrice == null || item.costPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Cost price must be zero or greater.");
        }
        LocalDate expiry = parseDate(item.expiryDate, true);
        if (expiry.isBefore(LocalDate.now())) {
            throw new ValidationException("Expiry date cannot be in the past for batch " + item.batchNo + ".");
        }
    }

    private LocalDate parseDate(String value, boolean required) {
        if (value == null || value.isBlank()) {
            if (required) {
                throw new ValidationException("Expiry date is required.");
            }
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid date (expected yyyy-MM-dd): " + value);
        }
    }

    private void rollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
            }
        }
    }

    private void close(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
