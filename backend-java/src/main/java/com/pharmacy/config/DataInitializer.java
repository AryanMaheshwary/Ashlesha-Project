package com.pharmacy.config;

import com.pharmacy.db.ConnectionManager;
import com.pharmacy.service.AuthService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Runs the DDL schema and seeds baseline data on startup. Idempotent:
 * users are created only if missing; sample catalog/sales are seeded only
 * when the medicine table is empty.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final AuthService authService;

    public DataInitializer(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void run(String... args) throws Exception {
        try (Connection conn = ConnectionManager.getConnection()) {
            runSchema(conn);
            seedUsers(conn);
            if (isMedicineTableEmpty(conn)) {
                seedSampleData(conn);
            }
            System.out.println("[DataInitializer] Database ready.");
        }
    }

    private void runSchema(Connection conn) throws Exception {
        String sql = StreamUtils.copyToString(
                new ClassPathResource("schema.sql").getInputStream(), StandardCharsets.UTF_8);
        StringBuilder cleaned = new StringBuilder();
        for (String line : sql.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                continue;
            }
            cleaned.append(line).append("\n");
        }
        try (Statement st = conn.createStatement()) {
            for (String stmt : cleaned.toString().split(";")) {
                if (!stmt.trim().isEmpty()) {
                    st.execute(stmt);
                }
            }
        }
    }

    private void seedUsers(Connection conn) throws SQLException {
        seedUser(conn, "admin", "admin123", "System Administrator", "ADMIN");
        seedUser(conn, "pharma", "pharma123", "Priya Pharmacist", "PHARMACIST");
        seedUser(conn, "cashier", "cashier123", "Charan Cashier", "CASHIER");
    }

    private void seedUser(Connection conn, String username, String password, String fullName, String role)
            throws SQLException {
        try (PreparedStatement check = conn.prepareStatement(
                "SELECT user_id FROM app_user WHERE username = ?")) {
            check.setString(1, username);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO app_user (username, password_hash, full_name, role) VALUES (?, ?, ?, ?)")) {
            ins.setString(1, username);
            ins.setString(2, authService.hash(password));
            ins.setString(3, fullName);
            ins.setString(4, role);
            ins.executeUpdate();
        }
    }

    private boolean isMedicineTableEmpty(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM medicine");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    private void seedSampleData(Connection conn) throws SQLException {
        int adminId = userId(conn, "admin");
        int pharmaId = userId(conn, "pharma");
        int cashierId = userId(conn, "cashier");

        int s1 = insertSupplier(conn, "MediCorp Distributors", "9876543210", "sales@medicorp.com", "12 Industrial Rd, Pune");
        insertSupplier(conn, "HealthPlus Wholesale", "9822011223", "orders@healthplus.com", "5 Market St, Mumbai");
        insertSupplier(conn, "PharmaDirect Ltd", "9811122233", "contact@pharmadirect.com", "88 Ring Rd, Delhi");

        int c1 = insertCustomer(conn, "Rahul Sharma", "9000000001", "21 Green Park, Pune");
        int c2 = insertCustomer(conn, "Anita Desai", "9000000002", "7 Lake View, Mumbai");
        int c3 = insertCustomer(conn, "Vikram Nair", "9000000003", "3 Hill Road, Nashik");

        int m1 = insertMedicine(conn, "Paracetamol 500mg", "Analgesic", "Cipla", "tablet", "2.50", false);
        int m2 = insertMedicine(conn, "Amoxicillin 250mg", "Antibiotic", "Sun Pharma", "capsule", "8.00", true);
        int m3 = insertMedicine(conn, "Cetirizine 10mg", "Antihistamine", "Dr. Reddy's", "tablet", "3.00", false);
        int m4 = insertMedicine(conn, "Ibuprofen 400mg", "Analgesic", "Abbott", "tablet", "4.00", false);
        int m5 = insertMedicine(conn, "Azithromycin 500mg", "Antibiotic", "Cipla", "tablet", "25.00", true);
        int m6 = insertMedicine(conn, "Vitamin C 1000mg", "Supplement", "HealthKart", "tablet", "5.00", false);
        int m7 = insertMedicine(conn, "Cough Syrup 100ml", "Respiratory", "Glenmark", "bottle", "60.00", false);
        int m8 = insertMedicine(conn, "Insulin Glargine", "Antidiabetic", "NovoNordisk", "vial", "350.00", true);

        int b1 = insertBatch(conn, m1, "PARA-A1", 400, 500);
        int b2 = insertBatch(conn, m1, "PARA-B2", 20, 300);   // expiring soon
        int b3 = insertBatch(conn, m2, "AMOX-C1", 200, 120);
        int b4 = insertBatch(conn, m3, "CETI-D1", 15, 60);    // expiring soon
        int b5 = insertBatch(conn, m4, "IBUP-E1", 365, 200);
        insertBatch(conn, m5, "AZIT-F1", 90, 40);
        int b7 = insertBatch(conn, m6, "VITC-G1", 300, 15);   // low stock
        int b8 = insertBatch(conn, m7, "COUGH-H1", 180, 8);   // low stock
        insertBatch(conn, m8, "INSU-I1", 25, 5);              // low stock + expiring

        int pr1 = insertPrescription(conn, c1, "Dr. Rao", "presc-rx-1001.pdf");

        // Historical + today's sales so revenue and top-selling reports have data.
        seedSale(conn, c1, adminId, null, days(-25), "CASH",
                new int[][]{{m1, b2, 20}, {m4, b5, 10}}, new String[]{"2.50", "4.00"});
        seedSale(conn, c2, pharmaId, null, days(-18), "CARD",
                new int[][]{{m3, b4, 10}, {m6, b7, 3}}, new String[]{"3.00", "5.00"});
        seedSale(conn, null, cashierId, null, days(-10), "UPI",
                new int[][]{{m1, b1, 15}}, new String[]{"2.50"});
        seedSale(conn, c3, cashierId, null, days(-3), "CASH",
                new int[][]{{m4, b5, 20}, {m7, b8, 2}}, new String[]{"4.00", "60.00"});
        seedSale(conn, c1, cashierId, pr1, days(0), "CASH",
                new int[][]{{m2, b3, 5}, {m1, b2, 5}}, new String[]{"8.00", "2.50"});
        seedSale(conn, c2, pharmaId, null, days(0), "CARD",
                new int[][]{{m3, b4, 8}}, new String[]{"3.00"});
    }

    private LocalDateTime days(int delta) {
        return LocalDateTime.now().plusDays(delta);
    }

    private int userId(Connection conn, String username) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM app_user WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private int insertSupplier(Connection conn, String name, String contact, String email, String address)
            throws SQLException {
        return insert(conn, "INSERT INTO supplier (name, contact_no, email, address) VALUES (?, ?, ?, ?)",
                name, contact, email, address);
    }

    private int insertCustomer(Connection conn, String name, String phone, String address) throws SQLException {
        return insert(conn, "INSERT INTO customer (name, phone, address) VALUES (?, ?, ?)", name, phone, address);
    }

    private int insertMedicine(Connection conn, String name, String category, String manufacturer, String unit,
                               String price, boolean presc) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO medicine (name, category, manufacturer, unit, unit_price, prescription_required) " +
                        "VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setString(3, manufacturer);
            ps.setString(4, unit);
            ps.setBigDecimal(5, new BigDecimal(price));
            ps.setBoolean(6, presc);
            ps.executeUpdate();
            return key(ps);
        }
    }

    private int insertBatch(Connection conn, int medicineId, String batchNo, int expiryOffsetDays, int qty)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO stock_batch (medicine_id, batch_no, manufacture_date, expiry_date, quantity_available) " +
                        "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, medicineId);
            ps.setString(2, batchNo);
            ps.setDate(3, java.sql.Date.valueOf(LocalDate.now().minusMonths(2)));
            ps.setDate(4, java.sql.Date.valueOf(LocalDate.now().plusDays(expiryOffsetDays)));
            ps.setInt(5, qty);
            ps.executeUpdate();
            return key(ps);
        }
    }

    private int insertPrescription(Connection conn, int customerId, String doctor, String fileRef)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO prescription (customer_id, doctor_name, issue_date, file_reference) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, customerId);
            ps.setString(2, doctor);
            ps.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
            ps.setString(4, fileRef);
            ps.executeUpdate();
            return key(ps);
        }
    }

    private void seedSale(Connection conn, Integer customerId, int userId, Integer prescriptionId,
                          LocalDateTime when, String paymentMode, int[][] items, String[] prices)
            throws SQLException {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < items.length; i++) {
            total = total.add(new BigDecimal(prices[i]).multiply(BigDecimal.valueOf(items[i][2])));
        }
        int saleId;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sale (customer_id, user_id, prescription_id, sale_date, total_amount, payment_mode) " +
                        "VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            if (customerId == null) {
                ps.setNull(1, java.sql.Types.INTEGER);
            } else {
                ps.setInt(1, customerId);
            }
            ps.setInt(2, userId);
            if (prescriptionId == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setInt(3, prescriptionId);
            }
            ps.setTimestamp(4, Timestamp.valueOf(when));
            ps.setBigDecimal(5, total);
            ps.setString(6, paymentMode);
            ps.executeUpdate();
            saleId = key(ps);
        }
        for (int i = 0; i < items.length; i++) {
            int medicineId = items[i][0];
            int batchId = items[i][1];
            int qty = items[i][2];
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO sale_item (sale_id, medicine_id, batch_id, quantity, selling_price) " +
                            "VALUES (?, ?, ?, ?, ?)")) {
                ps.setInt(1, saleId);
                ps.setInt(2, medicineId);
                ps.setInt(3, batchId);
                ps.setInt(4, qty);
                ps.setBigDecimal(5, new BigDecimal(prices[i]));
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE stock_batch SET quantity_available = quantity_available - ? WHERE batch_id = ?")) {
                ps.setInt(1, qty);
                ps.setInt(2, batchId);
                ps.executeUpdate();
            }
        }
    }

    private int insert(Connection conn, String sql, String... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            ps.executeUpdate();
            return key(ps);
        }
    }

    private int key(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.getGeneratedKeys()) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }
}
