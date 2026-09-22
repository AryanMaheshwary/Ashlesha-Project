package com.pharmacy.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Central JDBC connection provider. All database access in the app goes through
 * raw java.sql connections obtained here (no ORM / no connection pool magic that
 * hides the SQL). Configuration is read from environment variables with sensible
 * local defaults so the project runs out of the box for a college setup.
 */
public final class ConnectionManager {

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        URL = env("DB_URL",
                "jdbc:mysql://127.0.0.1:3306/pharmacy_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        USER = env("DB_USER", "pharmacy");
        PASSWORD = env("DB_PASSWORD", "pharmacy");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC driver not found", e);
        }
    }

    private ConnectionManager() {
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? fallback : v;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
