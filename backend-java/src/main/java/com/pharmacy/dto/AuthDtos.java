package com.pharmacy.dto;

import com.pharmacy.model.User;

public class AuthDtos {

    public static class LoginRequest {
        public String username;
        public String password;
    }

    public static class LoginResponse {
        public String token;
        public User user;

        public LoginResponse(String token, User user) {
            this.token = token;
            this.user = user;
        }
    }
}
