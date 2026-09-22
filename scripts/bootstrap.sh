#!/bin/bash
# Idempotent bootstrap for the preview environment. Ensures MariaDB (persistent
# datadir under /app) and the Java Spring Boot backend (port 8090) are running.
# Invoked from the persistent uvicorn 'backend' program on startup, so the app
# self-heals across pod restarts.

DATADIR=/app/data/mysql
SOCK=/var/run/mysqld/mysqld.sock
JAR=/app/backend-java/target/pharmacy-backend.jar

mkdir -p /var/run/mysqld
chown -R mysql:mysql /var/run/mysqld 2>/dev/null || true

# 1) MariaDB ---------------------------------------------------------------
if ! mysqladmin --socket=$SOCK ping >/dev/null 2>&1; then
  echo "[bootstrap] starting MariaDB (datadir=$DATADIR)"
  if [ ! -d "$DATADIR/mysql" ]; then
    mariadb-install-db --user=mysql --datadir=$DATADIR --auth-root-authentication-method=normal >/app/data/mysql-init.log 2>&1
  fi
  chown -R mysql:mysql $DATADIR 2>/dev/null || true
  setsid /usr/sbin/mariadbd --user=mysql --datadir=$DATADIR --socket=$SOCK \
      --port=3306 --bind-address=127.0.0.1 >/app/data/mariadb.log 2>&1 &
  for i in $(seq 1 30); do
    mysqladmin --socket=$SOCK ping >/dev/null 2>&1 && break
    sleep 1
  done
fi

# Ensure application database + user exist.
mysql --socket=$SOCK -u root <<'SQL' 2>/dev/null || true
CREATE DATABASE IF NOT EXISTS pharmacy_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'pharmacy'@'localhost' IDENTIFIED BY 'pharmacy';
CREATE USER IF NOT EXISTS 'pharmacy'@'127.0.0.1' IDENTIFIED BY 'pharmacy';
GRANT ALL PRIVILEGES ON pharmacy_db.* TO 'pharmacy'@'localhost';
GRANT ALL PRIVILEGES ON pharmacy_db.* TO 'pharmacy'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL

# 2) Java backend ----------------------------------------------------------
if ! curl -sf http://127.0.0.1:8090/api/health >/dev/null 2>&1; then
  if [ -f "$JAR" ]; then
    echo "[bootstrap] starting Java backend on :8090"
    cd /app/backend-java
    SERVER_PORT=8090 \
    DB_URL="jdbc:mysql://127.0.0.1:3306/pharmacy_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
    DB_USER=pharmacy DB_PASSWORD=pharmacy \
    setsid java -jar "$JAR" >/app/data/javabackend.log 2>&1 &
  else
    echo "[bootstrap] Java jar not built yet; skipping backend start"
  fi
fi
echo "[bootstrap] done"
