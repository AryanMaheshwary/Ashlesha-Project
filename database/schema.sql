-- ============================================================================
--  Pharmacy Management System - MySQL / MariaDB Schema (normalized to 3NF)
--  Run:  mysql -u root -p < database/schema.sql
-- ============================================================================

CREATE DATABASE IF NOT EXISTS pharmacy_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pharmacy_db;

-- Drop in FK-safe (child -> parent) order for a clean re-create.
DROP TABLE IF EXISTS sale_item;
DROP TABLE IF EXISTS sale;
DROP TABLE IF EXISTS purchase_item;
DROP TABLE IF EXISTS purchase;
DROP TABLE IF EXISTS prescription;
DROP TABLE IF EXISTS customer;
DROP TABLE IF EXISTS stock_batch;
DROP TABLE IF EXISTS medicine;
DROP TABLE IF EXISTS supplier;
DROP TABLE IF EXISTS app_user;

-- ---------------------------------------------------------------------------
-- Users (authentication + role based access)
-- ---------------------------------------------------------------------------
CREATE TABLE app_user (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,             -- BCrypt hash
    full_name     VARCHAR(100) NOT NULL,
    role          ENUM('ADMIN','PHARMACIST','CASHIER') NOT NULL,
    CONSTRAINT uq_user_username UNIQUE (username)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Suppliers
-- ---------------------------------------------------------------------------
CREATE TABLE supplier (
    supplier_id INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    contact_no  VARCHAR(20),
    email       VARCHAR(120),
    address     VARCHAR(255)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Medicine catalog
-- ---------------------------------------------------------------------------
CREATE TABLE medicine (
    medicine_id           INT AUTO_INCREMENT PRIMARY KEY,
    name                  VARCHAR(120)  NOT NULL,
    category              VARCHAR(60)   NOT NULL,
    manufacturer          VARCHAR(120),
    unit                  VARCHAR(30)   NOT NULL,
    unit_price            DECIMAL(10,2) NOT NULL,
    prescription_required BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_medicine_price CHECK (unit_price >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Stock batches (per-batch expiry so the same medicine can differ by batch)
-- ---------------------------------------------------------------------------
CREATE TABLE stock_batch (
    batch_id           INT AUTO_INCREMENT PRIMARY KEY,
    medicine_id        INT         NOT NULL,
    batch_no           VARCHAR(60) NOT NULL,
    manufacture_date   DATE,
    expiry_date        DATE        NOT NULL,
    quantity_available INT         NOT NULL,
    CONSTRAINT fk_batch_medicine FOREIGN KEY (medicine_id) REFERENCES medicine(medicine_id),
    CONSTRAINT chk_batch_qty CHECK (quantity_available >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Customers
-- ---------------------------------------------------------------------------
CREATE TABLE customer (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    phone       VARCHAR(20),
    address     VARCHAR(255)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Prescriptions
-- ---------------------------------------------------------------------------
CREATE TABLE prescription (
    prescription_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id     INT          NOT NULL,
    doctor_name     VARCHAR(120) NOT NULL,
    issue_date      DATE         NOT NULL,
    file_reference  VARCHAR(255),
    CONSTRAINT fk_presc_customer FOREIGN KEY (customer_id) REFERENCES customer(customer_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Purchases (incoming stock header + lines)
-- ---------------------------------------------------------------------------
CREATE TABLE purchase (
    purchase_id   INT AUTO_INCREMENT PRIMARY KEY,
    supplier_id   INT           NOT NULL,
    user_id       INT           NOT NULL,
    purchase_date DATETIME      NOT NULL,
    total_amount  DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_purchase_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(supplier_id),
    CONSTRAINT fk_purchase_user     FOREIGN KEY (user_id)     REFERENCES app_user(user_id)
) ENGINE=InnoDB;

CREATE TABLE purchase_item (
    purchase_item_id INT AUTO_INCREMENT PRIMARY KEY,
    purchase_id      INT           NOT NULL,
    medicine_id      INT           NOT NULL,
    batch_id         INT           NOT NULL,
    quantity         INT           NOT NULL,
    cost_price       DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_pitem_purchase FOREIGN KEY (purchase_id) REFERENCES purchase(purchase_id),
    CONSTRAINT fk_pitem_medicine FOREIGN KEY (medicine_id) REFERENCES medicine(medicine_id),
    CONSTRAINT fk_pitem_batch    FOREIGN KEY (batch_id)    REFERENCES stock_batch(batch_id),
    CONSTRAINT chk_pitem_qty CHECK (quantity > 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Sales (invoice header + lines). customer_id & prescription_id are nullable.
-- ---------------------------------------------------------------------------
CREATE TABLE sale (
    sale_id         INT AUTO_INCREMENT PRIMARY KEY,
    customer_id     INT           NULL,
    user_id         INT           NOT NULL,
    prescription_id INT           NULL,
    sale_date       DATETIME      NOT NULL,
    total_amount    DECIMAL(12,2) NOT NULL,
    payment_mode    ENUM('CASH','CARD','UPI') NOT NULL,
    CONSTRAINT fk_sale_customer FOREIGN KEY (customer_id)     REFERENCES customer(customer_id),
    CONSTRAINT fk_sale_user     FOREIGN KEY (user_id)         REFERENCES app_user(user_id),
    CONSTRAINT fk_sale_presc    FOREIGN KEY (prescription_id) REFERENCES prescription(prescription_id)
) ENGINE=InnoDB;

CREATE TABLE sale_item (
    sale_item_id  INT AUTO_INCREMENT PRIMARY KEY,
    sale_id       INT           NOT NULL,
    medicine_id   INT           NOT NULL,
    batch_id      INT           NOT NULL,
    quantity      INT           NOT NULL,
    selling_price DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_sitem_sale     FOREIGN KEY (sale_id)     REFERENCES sale(sale_id),
    CONSTRAINT fk_sitem_medicine FOREIGN KEY (medicine_id) REFERENCES medicine(medicine_id),
    CONSTRAINT fk_sitem_batch    FOREIGN KEY (batch_id)    REFERENCES stock_batch(batch_id),
    CONSTRAINT chk_sitem_qty CHECK (quantity > 0)
) ENGINE=InnoDB;

-- Helpful indexes for reporting / FIFO lookups
CREATE INDEX idx_batch_medicine_expiry ON stock_batch (medicine_id, expiry_date);
CREATE INDEX idx_sale_date ON sale (sale_date);
CREATE INDEX idx_sale_item_medicine ON sale_item (medicine_id);
