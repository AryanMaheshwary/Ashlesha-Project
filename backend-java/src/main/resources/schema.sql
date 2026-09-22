-- Pharmacy Management System - MySQL / MariaDB schema (normalized to 3NF)
-- Safe to run repeatedly: uses CREATE TABLE IF NOT EXISTS.

CREATE TABLE IF NOT EXISTS app_user (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    role          ENUM('ADMIN','PHARMACIST','CASHIER') NOT NULL,
    CONSTRAINT uq_user_username UNIQUE (username)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS supplier (
    supplier_id INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    contact_no  VARCHAR(20),
    email       VARCHAR(120),
    address     VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS medicine (
    medicine_id           INT AUTO_INCREMENT PRIMARY KEY,
    name                  VARCHAR(120)  NOT NULL,
    category              VARCHAR(60)   NOT NULL,
    manufacturer          VARCHAR(120),
    unit                  VARCHAR(30)   NOT NULL,
    unit_price            DECIMAL(10,2) NOT NULL,
    prescription_required BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_medicine_price CHECK (unit_price >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stock_batch (
    batch_id           INT AUTO_INCREMENT PRIMARY KEY,
    medicine_id        INT         NOT NULL,
    batch_no           VARCHAR(60) NOT NULL,
    manufacture_date   DATE,
    expiry_date        DATE        NOT NULL,
    quantity_available INT         NOT NULL,
    CONSTRAINT fk_batch_medicine FOREIGN KEY (medicine_id) REFERENCES medicine(medicine_id),
    CONSTRAINT chk_batch_qty CHECK (quantity_available >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS customer (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    phone       VARCHAR(20),
    address     VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS prescription (
    prescription_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id     INT          NOT NULL,
    doctor_name     VARCHAR(120) NOT NULL,
    issue_date      DATE         NOT NULL,
    file_reference  VARCHAR(255),
    CONSTRAINT fk_presc_customer FOREIGN KEY (customer_id) REFERENCES customer(customer_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS purchase (
    purchase_id   INT AUTO_INCREMENT PRIMARY KEY,
    supplier_id   INT           NOT NULL,
    user_id       INT           NOT NULL,
    purchase_date DATETIME      NOT NULL,
    total_amount  DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_purchase_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(supplier_id),
    CONSTRAINT fk_purchase_user     FOREIGN KEY (user_id)     REFERENCES app_user(user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS purchase_item (
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

CREATE TABLE IF NOT EXISTS sale (
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

CREATE TABLE IF NOT EXISTS sale_item (
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
