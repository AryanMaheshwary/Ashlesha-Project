package com.pharmacy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class User {
    public Integer userId;
    public String username;
    @JsonIgnore
    public String passwordHash;
    public String fullName;
    public String role;
}
