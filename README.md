# 💊 Pharmacy Management System

A full-stack Pharmacy Management System with a **Java (Spring Boot + raw JDBC)** backend,
a **MySQL / MariaDB** relational database (3NF), and a **React** frontend.

> The backend uses **raw JDBC with `PreparedStatement`s** and a **DAO + Service** layering.
> There is **no ORM** (no JPA/Hibernate) — every SQL statement is hand-written and visible in the DAO classes.

---

## ✨ Features

- **Role-based login** (ADMIN / PHARMACIST / CASHIER) with **BCrypt**-hashed passwords and in-memory sessions.
- **Medicine catalog** CRUD with category + prescription-required flag.
- **Stock batch entry** — record supplier purchases; each line creates a new batch with its own expiry date.
- **Billing / POS** — search medicines, build a cart, and generate an invoice. Stock is deducted
  **FIFO by nearest expiry**, inside a **single DB transaction** (commit/rollback — never a partial deduction).
- **Prescription safety** — prescription-only medicines cannot be sold without a linked prescription.
- **Reports** — low-stock alerts, expiry alerts (≤ 30 days), daily/monthly revenue, top-selling medicines.
- **Supplier & customer** management (CRUD).
- **Custom exceptions** for insufficient stock, expired batch, and missing prescription.

---

## 🧱 Tech Stack

| Layer     | Technology                                        |
|-----------|---------------------------------------------------|
| Backend   | Java 17, Spring Boot 3.2 (Web), **raw JDBC**      |
| Database  | MySQL 8 / MariaDB 10.11 (MySQL Connector/J)       |
| Frontend  | React 19, Tailwind CSS, Recharts, lucide-react    |
| Auth      | BCrypt password hashing + in-memory session token |

---

## 📁 Project Structure

```
/app
├── database/
│   ├── schema.sql          # CREATE DATABASE + all tables + constraints (3NF)
│   └── seed.sql            # Optional sample data (users are BCrypt-hashed)
├── backend-java/           # Java Spring Boot backend (the real backend)
│   ├── pom.xml
│   └── src/main/java/com/pharmacy/
│       ├── controller/     # REST controllers (no SQL here)
│       ├── service/        # Business logic (stock validation, transactions, totals)
│       ├── dao/            # DAO classes — all SQL via PreparedStatement
│       ├── model/          # Entity POJOs
│       ├── dto/            # Request/response DTOs
│       ├── db/             # ConnectionManager (JDBC)
│       ├── security/       # Session manager, @Auth interceptor
│       ├── exception/      # Custom exceptions + global handler
│       └── config/         # CORS, interceptor wiring, data seeding
└── frontend/               # React app (login, inventory, billing, reports…)
```

---

## ✅ Prerequisites

- **Java 17+** and **Maven 3.8+**  (`java -version`, `mvn -version`)
- **MySQL 8** or **MariaDB 10+**   (`mysql --version`)
- **Node 18+** and **Yarn**        (`node -v`, `yarn -v`)

---

## 🚀 Run Locally

### 1) Database

```bash
# Create schema + tables
mysql -u root -p < database/schema.sql

# (Optional) load sample data — OR let the backend auto-seed on first run
mysql -u root -p pharmacy_db < database/seed.sql

# Create the app DB user the backend expects (or use your own — see step 2)
mysql -u root -p -e "
CREATE USER IF NOT EXISTS 'pharmacy'@'localhost' IDENTIFIED BY 'pharmacy';
GRANT ALL PRIVILEGES ON pharmacy_db.* TO 'pharmacy'@'localhost';
FLUSH PRIVILEGES;"
```

### 2) Backend (Java)

The backend reads DB config from **environment variables** (with sensible defaults):

| Variable      | Default                                                        |
|---------------|----------------------------------------------------------------|
| `DB_URL`      | `jdbc:mysql://127.0.0.1:3306/pharmacy_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `DB_USER`     | `pharmacy`                                                     |
| `DB_PASSWORD` | `pharmacy`                                                     |
| `SERVER_PORT` | `8080`                                                         |

```bash
cd backend-java
mvn clean package -DskipTests
java -jar target/pharmacy-backend.jar
# → starts on http://localhost:8080
# On first run against an empty DB it auto-creates tables and seeds sample data.
```

Verify: `curl http://localhost:8080/api/health`

### 3) Frontend (React)

Point the frontend at the backend by setting `REACT_APP_BACKEND_URL` in `frontend/.env`
(the app calls `${REACT_APP_BACKEND_URL}/api/...`). For a local backend on port 8080:

```bash
# frontend/.env
REACT_APP_BACKEND_URL=http://localhost:8080
```

```bash
cd frontend
yarn install
yarn start
# → opens http://localhost:3000
```

---

## 🔑 Default Logins

| Role       | Username  | Password    |
|------------|-----------|-------------|
| Admin      | `admin`   | `admin123`  |
| Pharmacist | `pharma`  | `pharma123` |
| Cashier    | `cashier` | `cashier123`|

---

## 🔌 API Overview (prefix `/api`)

| Method | Path                         | Roles                         |
|--------|------------------------------|-------------------------------|
| POST   | `/auth/login`                | public                        |
| GET    | `/auth/me`                   | any authenticated             |
| GET/POST/PUT/DELETE | `/medicines`    | read: all · write: ADMIN/PHARMACIST · delete: ADMIN |
| GET    | `/batches?medicineId=`       | any authenticated             |
| GET/POST | `/purchases`               | ADMIN / PHARMACIST            |
| GET/POST/PUT/DELETE | `/suppliers`    | ADMIN / PHARMACIST            |
| GET/POST/PUT/DELETE | `/customers`    | any authenticated             |
| GET/POST | `/prescriptions`           | any authenticated             |
| GET/POST | `/sales`, `/sales/{id}`    | any authenticated             |
| GET    | `/reports/*`                 | ADMIN / PHARMACIST            |

Auth: send the login token as `Authorization: Bearer <token>`.

---

## 🧠 Design Notes

- **Transactions:** `SaleService.createSale` opens one JDBC connection, sets
  `autoCommit(false)`, validates + deducts every batch, then commits. Any failure
  (insufficient stock, expired-only stock, missing prescription) triggers a rollback.
- **FIFO by expiry:** billing consumes stock from `stock_batch` ordered by
  `expiry_date ASC`, skipping already-expired batches.
- **SQL injection safe:** 100% `PreparedStatement`; no string concatenation of user input.
- **DB engine:** MySQL and MariaDB are wire/SQL compatible; the schema and JDBC work on either.
