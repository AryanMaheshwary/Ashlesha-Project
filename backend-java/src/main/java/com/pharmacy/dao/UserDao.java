package com.pharmacy.dao;

import com.pharmacy.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public User findByUsername(Connection conn, String username) throws SQLException {
        String sql = "SELECT user_id, username, password_hash, full_name, role FROM app_user WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public User findById(Connection conn, int id) throws SQLException {
        String sql = "SELECT user_id, username, password_hash, full_name, role FROM app_user WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<User> findAll(Connection conn) throws SQLException {
        String sql = "SELECT user_id, username, password_hash, full_name, role FROM app_user ORDER BY user_id";
        List<User> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public int insert(Connection conn, User u) throws SQLException {
        String sql = "INSERT INTO app_user (username, password_hash, full_name, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.username);
            ps.setString(2, u.passwordHash);
            ps.setString(3, u.fullName);
            ps.setString(4, u.role);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.userId = rs.getInt("user_id");
        u.username = rs.getString("username");
        u.passwordHash = rs.getString("password_hash");
        u.fullName = rs.getString("full_name");
        u.role = rs.getString("role");
        return u;
    }
}
