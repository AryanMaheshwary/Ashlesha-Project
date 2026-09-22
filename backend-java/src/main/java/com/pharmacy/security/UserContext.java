package com.pharmacy.security;

import com.pharmacy.exception.AuthException;
import com.pharmacy.model.User;
import jakarta.servlet.http.HttpServletRequest;

public final class UserContext {

    public static final String ATTR = "authUser";

    private UserContext() {
    }

    public static User current(HttpServletRequest request) {
        User u = (User) request.getAttribute(ATTR);
        if (u == null) {
            throw new AuthException("Not authenticated");
        }
        return u;
    }
}
