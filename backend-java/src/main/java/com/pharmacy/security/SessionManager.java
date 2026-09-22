package com.pharmacy.security;

import com.pharmacy.model.User;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Simple in-memory session store: opaque token -> authenticated user. */
@Component
public class SessionManager {

    private final Map<String, User> sessions = new ConcurrentHashMap<>();

    public String create(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, user);
        return token;
    }

    public User get(String token) {
        return token == null ? null : sessions.get(token);
    }

    public void remove(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }
}
