package com.github.datlaipro.docflow.api.auth.repo;



import java.sql.Timestamp;

public class RefreshTokenRow {
    public long id;
    public long userId;
    public String jti;
    public String familyId;
    public String tokenHash;
    public String deviceId;
    public String ip;
    public String ua;
    public Timestamp expiresAt;
    public java.sql.Timestamp revokedAt;

    public boolean isRevoked() { return revokedAt != null; }
}
