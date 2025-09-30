package com.github.datlaipro.docflow.api.auth.repo;

import com.github.datlaipro.docflow.api.auth.infra.Db;

import java.sql.*;

public class RefreshTokenDaoImpl implements RefreshTokenDao {
    @Override
    public void insert(long userId, String jti, String familyId, String tokenHash, String deviceId,
                       String ip, String ua, Timestamp expiresAt) throws Exception {
        String sql = "INSERT INTO refresh_tokens(user_id,jti,family_id,token_hash,device_id,ip_address,user_agent,expires_at,created_at) " +
                     "VALUES(?,?,?,?,?,?,?, ?, NOW())";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, jti);
            ps.setString(3, familyId);
            ps.setString(4, tokenHash);
            ps.setString(5, deviceId);
            ps.setString(6, ip);
            ps.setString(7, ua);
            ps.setTimestamp(8, expiresAt);
            ps.executeUpdate();
        }
    }

    @Override
    public RefreshTokenRow findActiveByHash(String tokenHash) throws Exception {
        String sql = "SELECT id,user_id,jti,family_id,token_hash,device_id,ip_address,user_agent,expires_at,revoked_at " +
                     "FROM refresh_tokens WHERE token_hash=? LIMIT 1";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                RefreshTokenRow r = new RefreshTokenRow();
                r.id = rs.getLong("id");
                r.userId = rs.getLong("user_id");
                r.jti = rs.getString("jti");
                r.familyId = rs.getString("family_id");
                r.tokenHash = rs.getString("token_hash");
                r.deviceId = rs.getString("device_id");
                r.ip = rs.getString("ip_address");
                r.ua = rs.getString("user_agent");
                r.expiresAt = rs.getTimestamp("expires_at");
                r.revokedAt = rs.getTimestamp("revoked_at");
                return r;
            }
        }
    }

    @Override
    public void revokeById(long id) throws Exception {
        String sql = "UPDATE refresh_tokens SET revoked_at=NOW() WHERE id=?";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public void revokeFamily(long userId, String familyId) throws Exception {
        String sql = "UPDATE refresh_tokens SET revoked_at=NOW() WHERE user_id=? AND family_id=? AND revoked_at IS NULL";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, familyId);
            ps.executeUpdate();
        }
    }
}
