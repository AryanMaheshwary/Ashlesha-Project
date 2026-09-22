package com.pharmacy.controller;

import com.pharmacy.dto.AuthDtos.LoginRequest;
import com.pharmacy.dto.AuthDtos.LoginResponse;
import com.pharmacy.model.User;
import com.pharmacy.security.Auth;
import com.pharmacy.security.SessionManager;
import com.pharmacy.security.UserContext;
import com.pharmacy.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SessionManager sessionManager;

    public AuthController(AuthService authService, SessionManager sessionManager) {
        this.authService = authService;
        this.sessionManager = sessionManager;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        User user = authService.login(req.username, req.password);
        String token = authService.createSession(user);
        return new LoginResponse(token, user);
    }

    @Auth
    @GetMapping("/me")
    public User me(HttpServletRequest request) {
        return UserContext.current(request);
    }

    @Auth
    @PostMapping("/logout")
    public java.util.Map<String, Object> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7).trim()
                : request.getHeader("X-Session-Token");
        sessionManager.remove(token);
        return java.util.Map.of("success", true);
    }
}
