package com.github.datlaipro.docflow.api.auth.dto;

public class MeResponse {
    public long userId;
    public String email;
    public String role;

    public MeResponse(long userId, String email, String role) {
        this.userId = userId; this.email = email; this.role = role;
    }
}
