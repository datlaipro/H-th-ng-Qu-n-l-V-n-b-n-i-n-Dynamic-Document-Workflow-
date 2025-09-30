package com.github.datlaipro.docflow.api.auth.entity;

public class User {
    public long id;
    public String email;
    public String fullName;
    public String role;   // EMPLOYEE | LEADER | ADMIN
    public boolean active;

    public User(long id, String email, String fullName, String role, boolean active) {
        this.id = id; this.email = email; this.fullName = fullName; this.role = role; this.active = active;
    }
}
