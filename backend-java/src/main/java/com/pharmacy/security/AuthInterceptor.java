package com.pharmacy.security;

import com.pharmacy.exception.AuthException;
import com.pharmacy.exception.ForbiddenException;
import com.pharmacy.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final SessionManager sessionManager;

    public AuthInterceptor(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        Auth auth = handlerMethod.getMethodAnnotation(Auth.class);
        if (auth == null) {
            auth = handlerMethod.getBeanType().getAnnotation(Auth.class);
        }
        if (auth == null) {
            return true; // public endpoint
        }

        User user = sessionManager.get(extractToken(request));
        if (user == null) {
            throw new AuthException("Authentication required. Please log in.");
        }

        if (auth.roles().length > 0 && Arrays.stream(auth.roles()).noneMatch(r -> r.equalsIgnoreCase(user.role))) {
            throw new ForbiddenException("Your role (" + user.role + ") is not permitted to perform this action.");
        }

        request.setAttribute(UserContext.ATTR, user);
        return true;
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return request.getHeader("X-Session-Token");
    }
}
