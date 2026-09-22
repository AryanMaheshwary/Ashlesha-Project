package com.pharmacy.service;

import com.pharmacy.dao.MedicineDao;
import com.pharmacy.dao.PrescriptionDao;
import com.pharmacy.dao.SaleDao;
import com.pharmacy.dao.StockBatchDao;
import com.pharmacy.db.ConnectionManager;
import com.pharmacy.dto.SaleRequest;
import com.pharmacy.exception.ExpiredBatchException;
import com.pharmacy.exception.InsufficientStockException;
import com.pharmacy.exception.MissingPrescriptionException;
import com.pharmacy.exception.NotFoundException;
import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.Sale;
import com.pharmacy.model.SaleItem;
import com.pharmacy.model.StockBatch;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class SaleService {

    private static final Set<String> PAYMENT_MODES = Set.of("CASH", "CARD", "UPI");

    private final MedicineDao medicineDao = new MedicineDao();
    private final StockBatchDao batchDao = new StockBatchDao();
    private final SaleDao saleDao = new SaleDao();
    private final PrescriptionDao prescriptionDao = new PrescriptionDao();

    public List<Sale> list() {
        try (Connection conn = ConnectionManager.getConnection()) {
            return saleDao.findAll(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Sale get(int id) {
        try (Connection conn = ConnectionManager.getConnection()) {
            Sale sale = saleDao.findById(conn, id);
            if (sale == null) {
                throw new NotFoundException("Sale not found: " + id);
            }
            return sale;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Creates a sale/invoice. All stock validation and deduction happens inside a single
     * transaction: any failure (insufficient stock, expired batch, missing prescription)
     * rolls back so no partial deduction ever occurs. Stock is consumed FIFO by expiry date.
     */
    public Sale createSale(int userId, SaleRequest req) {
        validateRequest(req);
        String paymentMode = req.paymentMode.trim().toUpperCase();
        if (!PAYMENT_MODES.contains(paymentMode)) {
            throw new ValidationException("Invalid payment mode. Use CASH, CARD or UPI.");
        }

        Connection conn = null;
        try {
            conn = ConnectionManager.getConnection();
            conn.setAutoCommit(false);

            // Validate prescription reference if provided.
            if (req.prescriptionId != null && prescriptionDao.findById(conn, req.prescriptionId) == null) {
                throw new ValidationException("Prescription not found: " + req.prescriptionId);
            }

            List<SaleItem> saleItems = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;

            for (SaleRequest.Item line : req.items) {
                Medicine med = medicineDao.findById(conn, line.medicineId);
                if (med == null) {
                    throw new NotFoundException("Medicine not found: " + line.medicineId);
                }
                if (med.prescriptionRequired && req.prescriptionId == null) {
                    throw new MissingPrescriptionException(
                            "'" + med.name + "' requires a prescription. Please link a prescription to this sale.");
                }

                int remaining = line.quantity;
                List<StockBatch> sellable = batchDao.findSellableByMedicine(conn, line.medicineId);
                int available = sellable.stream().mapToInt(b -> b.quantityAvailable).sum();

                if (available < remaining) {
                    int expired = batchDao.expiredStock(conn, line.medicineId);
                    if (available == 0 && expired > 0) {
                        throw new ExpiredBatchException(
                                "All available stock for '" + med.name + "' is expired and cannot be sold.");
                    }
                    throw new InsufficientStockException(
                            "Insufficient stock for '" + med.name + "'. Requested " + line.quantity
                                    + ", only " + available + " available (non-expired).");
                }

                // FIFO consume from nearest-expiry batches first.
                for (StockBatch batch : sellable) {
                    if (remaining <= 0) {
                        break;
                    }
                    int take = Math.min(remaining, batch.quantityAvailable);
                    int updated = batchDao.decrementQuantity(conn, batch.batchId, take);
                    if (updated == 0) {
                        throw new InsufficientStockException(
                                "Stock changed while processing '" + med.name + "'. Please retry.");
                    }
                    SaleItem si = new SaleItem();
                    si.medicineId = med.medicineId;
                    si.batchId = batch.batchId;
                    si.quantity = take;
                    si.sellingPrice = med.unitPrice;
                    si.medicineName = med.name;
                    si.batchNo = batch.batchNo;
                    saleItems.add(si);
                    total = total.add(med.unitPrice.multiply(BigDecimal.valueOf(take)));
                    remaining -= take;
                }
            }

            Sale sale = new Sale();
            sale.customerId = req.customerId;
            sale.userId = userId;
            sale.prescriptionId = req.prescriptionId;
            sale.saleDate = LocalDateTime.now();
            sale.totalAmount = total;
            sale.paymentMode = paymentMode;
            int saleId = saleDao.insertSale(conn, sale);
            sale.saleId = saleId;

            for (SaleItem si : saleItems) {
                si.saleId = saleId;
                saleDao.insertItem(conn, si);
            }
            sale.items = saleItems;

            conn.commit();

            // Reload with joined names for a clean invoice response.
            try (Connection read = ConnectionManager.getConnection()) {
                return saleDao.findById(read, saleId);
            }
        } catch (RuntimeException | SQLException e) {
            rollback(conn);
            if (e instanceof com.pharmacy.exception.ApiException api) {
                throw api;
            }
            throw new RuntimeException("Sale failed: " + e.getMessage(), e);
        } finally {
            close(conn);
        }
    }

    private void validateRequest(SaleRequest req) {
        if (req == null || req.items == null || req.items.isEmpty()) {
            throw new ValidationException("Cart is empty. Add at least one medicine.");
        }
        if (req.paymentMode == null || req.paymentMode.isBlank()) {
            throw new ValidationException("Payment mode is required.");
        }
        for (SaleRequest.Item line : req.items) {
            if (line.medicineId == null) {
                throw new ValidationException("Each cart line must reference a medicine.");
            }
            if (line.quantity == null || line.quantity <= 0) {
                throw new ValidationException("Quantity must be greater than zero.");
            }
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
