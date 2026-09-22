# PRD — Pharmacy Management System

## Original Problem Statement
Build a full-stack Pharmacy Management System. STRICT: backend in **Java** using **JDBC** (no ORM hiding SQL);
database **MySQL** (3NF). Exact entities: app_user, supplier, medicine, stock_batch, customer, prescription,
purchase, purchase_item, sale, sale_item. Features: role-based login (ADMIN/PHARMACIST/CASHIER, hashed pw),
medicine CRUD, stock batch entry, billing with FIFO-by-expiry deduction + invoice, prescription linking,
reports (low-stock, expiry ≤30d, daily/monthly revenue, top-selling), supplier/customer CRUD.
Constraints: single-transaction stock deduction, PreparedStatements, DAO + service layering, custom exceptions.
Deliverables: runnable SQL schema, full Java backend, simple web frontend, README.

## Architecture (as built)
- **Backend:** Java 17 + Spring Boot 3.2 (Web) + **raw JDBC** (MySQL Connector/J). DAO → Service → Controller.
  BCrypt (spring-security-crypto) + in-memory session tokens. Runs on port 8090.
- **Database:** MariaDB (MySQL-compatible), schema `pharmacy_db`, datadir `/app/data/mysql` (persistent).
- **Frontend:** React 19 + Tailwind + Recharts. Talks to `${REACT_APP_BACKEND_URL}/api`.
- **Preview plumbing:** persistent uvicorn `backend` (port 8001) is a transparent proxy → Java:8090, and on
  startup runs `/app/scripts/bootstrap.sh` to (re)start MariaDB + the Java jar. NOT part of the deliverable.

## User Personas
- **Admin:** full access incl. deletes, reports, user of all modules.
- **Pharmacist:** catalog/stock/supplier management, reports, billing.
- **Cashier:** billing, customers, prescriptions, sales history (no reports/supplier/stock-entry).

## Core Requirements (static)
- Exact 10-table 3NF schema with FKs + CHECK constraints.
- FIFO-by-expiry stock deduction in ONE transaction (rollback on failure).
- Block sale on insufficient stock, expired-only stock, or missing prescription for Rx medicine.
- PreparedStatements everywhere; no SQL in controllers.

## Implemented (2026-06)
- ✅ Full Java backend (DAO/service/model/dto/controller/exception/security/config).
- ✅ Runnable `database/schema.sql` + `database/seed.sql`.
- ✅ Auth + RBAC (verified: 403 for cashier on reports, 401 unauth).
- ✅ Medicine/Supplier/Customer CRUD, Prescriptions, Purchases (batch entry).
- ✅ Billing with FIFO (verified nearest-expiry batch chosen), invoice, print.
- ✅ Reports: summary, low-stock, expiry, revenue (daily/monthly), top-selling (verified via curl).
- ✅ React frontend: Login, Dashboard, Billing, Inventory, Stock Entry, Suppliers, Customers, Prescriptions, Sales.
- ✅ README with local run instructions.

## Backlog / Next
- P1: Edit/void a sale; printable purchase invoice.
- P2: User management UI (create staff accounts) for Admin.
- P2: CSV export of reports; date-range filter on sales.
- P2: Pagination/serverside search on large tables.
