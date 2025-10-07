package com.github.datlaipro.docflow.api.auth.repo;

import com.github.datlaipro.docflow.api.auth.entity.User;
import infra.Db;

import java.sql.*;

public class UserDaoImpl implements UserDao {

    @Override
    public User findByEmail(String email) throws Exception {
        String sql = "SELECT id,email,full_name,role,is_active FROM users WHERE email=?";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    return null;
                return new User(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("full_name"),
                        rs.getString("role"),
                        rs.getBoolean("is_active"));
            }
        }
    }

    @Override
    public User findById(long id) throws Exception {
        String sql = "SELECT id,email,full_name,role,is_active FROM users WHERE id=?";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    return null;
                return new User(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("full_name"),
                        rs.getString("role"),
                        rs.getBoolean("is_active"));
            }
        }
    }

    @Override
    public String findPasswordHashByEmail(String email) throws Exception {
        String sql = "SELECT password_hash FROM users WHERE email=?";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    @Override
    public long createEmployee(String email, String fullName, String bcryptHash) throws Exception {
        String sql = "INSERT INTO users(email,password_hash,full_name,role,is_active,created_at,updated_at) " +
                "VALUES(?, ?, ?, 'EMPLOYEE', 1, NOW(), NOW())";
        try (Connection c = Db.get();
                PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, email);
            ps.setString(2, bcryptHash);
            ps.setString(3, fullName);
            if (ps.executeUpdate() == 0)
                throw new RuntimeException("Insert failed");
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    return rs.getLong(1);
                throw new RuntimeException("No key returned");
            }
        }
    }

 
}
