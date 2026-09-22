-- ============================================================================
--  Sample seed data for pharmacy_db
--  Run AFTER schema.sql:  mysql -u root -p pharmacy_db < database/seed.sql
--
--  NOTE: When you start the Java backend for the first time against an EMPTY
--  medicine table, it auto-seeds users + this same sample data. Use this file
--  only if you prefer a pure-SQL setup or want to reset the data.
--
--  Passwords (BCrypt hashed below):
--    admin   / admin123    (ADMIN)
--    pharma  / pharma123   (PHARMACIST)
--    cashier / cashier123  (CASHIER)
-- ============================================================================
USE pharmacy_db;

INSERT INTO app_user (username, password_hash, full_name, role) VALUES
 ('admin',   '$2b$10$00.ixi9ZzMjooWJi66TFGeUgOybQ.MOpdRlHoEVPENh5HMfwKVdTm', 'System Administrator', 'ADMIN'),
 ('pharma',  '$2b$10$jSaLpgdvV0AfS/yux0Z/leHbVuT6Co/14FjQH6LWSu2O4Xe9Yo/US', 'Priya Pharmacist',     'PHARMACIST'),
 ('cashier', '$2b$10$HAP3wIAb/3xcsypbozQ1cuObSYgDCGetCyIL6m9T8dna.bXZSKJJa', 'Charan Cashier',       'CASHIER');

INSERT INTO supplier (name, contact_no, email, address) VALUES
 ('MediCorp Distributors', '9876543210', 'sales@medicorp.com',    '12 Industrial Rd, Pune'),
 ('HealthPlus Wholesale',  '9822011223', 'orders@healthplus.com', '5 Market St, Mumbai'),
 ('PharmaDirect Ltd',      '9811122233', 'contact@pharmadirect.com','88 Ring Rd, Delhi');

INSERT INTO customer (name, phone, address) VALUES
 ('Rahul Sharma', '9000000001', '21 Green Park, Pune'),
 ('Anita Desai',  '9000000002', '7 Lake View, Mumbai'),
 ('Vikram Nair',  '9000000003', '3 Hill Road, Nashik');

INSERT INTO medicine (name, category, manufacturer, unit, unit_price, prescription_required) VALUES
 ('Paracetamol 500mg',  'Analgesic',     'Cipla',        'tablet',  2.50, FALSE),
 ('Amoxicillin 250mg',  'Antibiotic',    'Sun Pharma',   'capsule', 8.00, TRUE),
 ('Cetirizine 10mg',    'Antihistamine', 'Dr. Reddy''s', 'tablet',  3.00, FALSE),
 ('Ibuprofen 400mg',    'Analgesic',     'Abbott',       'tablet',  4.00, FALSE),
 ('Azithromycin 500mg', 'Antibiotic',    'Cipla',        'tablet', 25.00, TRUE),
 ('Vitamin C 1000mg',   'Supplement',    'HealthKart',   'tablet',  5.00, FALSE),
 ('Cough Syrup 100ml',  'Respiratory',   'Glenmark',     'bottle', 60.00, FALSE),
 ('Insulin Glargine',   'Antidiabetic',  'NovoNordisk',  'vial',  350.00, TRUE);

-- Batches (expiry relative to today; some expiring soon, some low quantity)
INSERT INTO stock_batch (medicine_id, batch_no, manufacture_date, expiry_date, quantity_available) VALUES
 (1, 'PARA-A1',  DATE_SUB(CURDATE(), INTERVAL 2 MONTH), DATE_ADD(CURDATE(), INTERVAL 400 DAY), 500),
 (1, 'PARA-B2',  DATE_SUB(CURDATE(), INTERVAL 2 MONTH), DATE_ADD(CURDATE(), INTERVAL 20  DAY), 300),
 (2, 'AMOX-C1',  DATE_SUB(CURDATE(), INTERVAL 2 MONTH), DATE_ADD(CURDATE(), INTERVAL 200 DAY), 120),
 (3, 'CETI-D1',  DATE_SUB(CURDATE(), INTERVAL 2 MONTH), DATE_ADD(CURDATE(), INTERVAL 15  DAY), 60),
 (4, 'IBUP-E1',  DATE_SUB(CURDATE(), INTERVAL 2 MONTH), DATE_ADD(CURDATE(), INTERVAL 365 DAY), 200),
 (5, 'AZIT-F1',  DATE_SUB(CURDATE(), INTERVAL 2 MONTH), DATE_ADD(CURDATE(), INTERVAL 90  DAY), 40),
 (6, 'VITC-G1',  DATE_SUB(CURDATE(), INTERVAL 2 MONTH), DATE_ADD(CURDATE(), INTERVAL 300 DAY), 15),
 (7, 'COUGH-H1', DATE_SUB(CURDATE(), INTERVAL 2 MONTH), DATE_ADD(CURDATE(), INTERVAL 180 DAY), 8),
 (8, 'INSU-I1',  DATE_SUB(CURDATE(), INTERVAL 2 MONTH), DATE_ADD(CURDATE(), INTERVAL 25  DAY), 5);

INSERT INTO prescription (customer_id, doctor_name, issue_date, file_reference) VALUES
 (1, 'Rao', CURDATE(), 'presc-rx-1001.pdf');
