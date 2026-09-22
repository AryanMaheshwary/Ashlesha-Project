package com.pharmacy.service;

import com.pharmacy.dao.UserDao;
import com.pharmacy.db.ConnectionManager;
import com.pharmacy.exception.AuthException;
import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.User;
import com.pharmacy.security.SessionManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;

@Service
public class AuthService {

    private final UserDao userDao = new UserDao();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SessionManager sessionManager;

    public AuthService(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public User login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new ValidationException("Username and password are required.");
        }
        try (Connection conn = ConnectionManager.getConnection()) {
            User user = userDao.findByUsername(conn, username.trim());
            if (user == null || !encoder.matches(password, user.passwordHash)) {
                throw new AuthException("Invalid username or password.");
            }
            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Login failed: " + e.getMessage(), e);
        }
    }

    public String createSession(User user) {
        return sessionManager.create(user);
    }

    public void logout(String token) {
        sessionManager.remove(token);
    }
}
