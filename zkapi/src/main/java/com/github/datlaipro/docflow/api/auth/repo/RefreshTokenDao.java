package com.github.datlaipro.docflow.api.auth.repo;



public interface RefreshTokenDao {
    void insert(long userId, String jti, String familyId, String tokenHash, String deviceId,
                String ip, String ua, java.sql.Timestamp expiresAt) throws Exception;

    RefreshTokenRow findActiveByHash(String tokenHash) throws Exception;

    void revokeById(long id) throws Exception;

    void revokeFamily(long userId, String familyId) throws Exception;
}
