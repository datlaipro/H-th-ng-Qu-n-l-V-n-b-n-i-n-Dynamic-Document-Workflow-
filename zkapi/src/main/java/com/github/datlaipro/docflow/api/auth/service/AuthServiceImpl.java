package com.github.datlaipro.docflow.api.auth.service;

import com.github.datlaipro.docflow.api.auth.dto.LoginRequest;
import com.github.datlaipro.docflow.api.auth.dto.RegisterRequest;
import com.github.datlaipro.docflow.api.auth.entity.User;
import com.github.datlaipro.docflow.api.auth.repo.*;
import com.github.datlaipro.docflow.api.auth.util.*;

import com.github.datlaipro.docflow.api.shared.error.AppException;
import com.github.datlaipro.docflow.api.shared.error.ErrorCode;
import com.github.datlaipro.docflow.api.auth.error.InvalidCredentialsException;
import com.github.datlaipro.docflow.api.auth.error.UserInactiveException;

import javax.servlet.http.*;
import java.sql.Timestamp;
import java.util.UUID;

public class AuthServiceImpl implements AuthService {
    private final UserDao userDao = new UserDaoImpl();// Tạo một thuộc tính chỉ dùng nội bộ, tham chiếu bất biến, kiểu
                                                      // interface UserDao, hiện đang được hiện thực bằng UserDaoImpl.”
    private final RefreshTokenDao rtDao = new RefreshTokenDaoImpl();

    @Override
    public void login(LoginRequest req, HttpServletRequest httpReq, HttpServletResponse httpResp) {
        try {
            User u = userDao.findByEmail(req.email);
            if (u == null)
                throw new InvalidCredentialsException();
            if (!u.active)
                throw new UserInactiveException();

            String hash = userDao.findPasswordHashByEmail(req.email);
            if (hash == null || !PasswordUtil.verify(req.password, hash)) {
                throw new InvalidCredentialsException();
            }

            long now = System.currentTimeMillis() / 1000L;
            long accessExp = now + 15 * 60; // 15 phút
            String access = JwtUtil.sign(u.id, u.role, u.email, accessExp);

            String familyId = UUID.randomUUID().toString();
            String jti = UUID.randomUUID().toString();
            String refreshPlain = UUID.randomUUID().toString() + "." + jti;
            String refreshHash = HashUtil.sha256Hex(refreshPlain);
            Timestamp rtExp = new Timestamp(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000);

            rtDao.insert(
                    u.id, jti, familyId, refreshHash, null,
                    httpReq.getRemoteAddr(), httpReq.getHeader("User-Agent"), rtExp);

            // === CORS cho response này (để browser nhận cookie cross-site) ===
            String origin = httpReq.getHeader("Origin");
            if (origin != null && origin.startsWith("http://localhost:4200")) {
                httpResp.setHeader("Access-Control-Allow-Origin", origin);
                httpResp.setHeader("Vary", "Origin");
                httpResp.setHeader("Access-Control-Allow-Credentials", "true");
            }

            // === Set-Cookie (dạng thủ công để có SameSite) ===
            int accessMaxAge = (int) (accessExp - now); // giây
            // ⚠ Với cross-site XHR: cần SameSite=None; Secure; => Hãy chạy HTTPS để cookie
            // không bị drop
            String sessionCookie = "SESSION=" + access
                    + "; Path=/"
                    + "; HttpOnly"
                    + "; Max-Age=" + accessMaxAge
                    + "; SameSite=None"
                    + "; Secure";
            httpResp.addHeader("Set-Cookie", sessionCookie);

            String refreshCookie = "REFRESH=" + refreshPlain
                    + "; Path=/api/auth"
                    + "; HttpOnly"
                    + "; Max-Age=" + (7 * 24 * 60 * 60)
                    + "; SameSite=None"
                    + "; Secure";
            httpResp.addHeader("Set-Cookie", refreshCookie);

        } catch (AppException ae) {
            throw ae;
        } catch (Throwable t) {
            throw new AppException(ErrorCode.DATABASE_ERROR, "Database error", t);
        }
    }

    @Override
    public long registerEmployee(RegisterRequest req) {
        try {
            String hash = PasswordUtil.hash(req.password);
            return userDao.createEmployee(req.email.trim(), req.fullName.trim(), hash);
        } catch (AppException ae) {
            throw ae;
        } catch (Throwable t) {
            throw new AppException(ErrorCode.DATABASE_ERROR, "Database error", t);
        }
    }

    @Override
    public void logout(HttpServletRequest httpReq, HttpServletResponse httpResp) {
        try {
            String refreshPlain = cookie(httpReq, "REFRESH");
            if (refreshPlain != null && !refreshPlain.isEmpty()) {
                RefreshTokenRow row = rtDao.findActiveByHash(HashUtil.sha256Hex(refreshPlain));
                if (row != null && !row.isRevoked())
                    rtDao.revokeById(row.id);
            }

            String origin = httpReq.getHeader("Origin");
            if (origin != null && origin.startsWith("http://localhost:4200")) {
                httpResp.setHeader("Access-Control-Allow-Origin", origin);
                httpResp.setHeader("Vary", "Origin");
                httpResp.setHeader("Access-Control-Allow-Credentials", "true");
            }

            // Max-Age=0 để xoá, same attributes
            httpResp.addHeader("Set-Cookie", "SESSION=; Path=/; Max-Age=0; HttpOnly; SameSite=None; Secure");
            httpResp.addHeader("Set-Cookie", "REFRESH=; Path=/api/auth; Max-Age=0; HttpOnly; SameSite=None; Secure");

        } catch (AppException ae) {
            throw ae;
        } catch (Throwable t) {
            throw new AppException(ErrorCode.DATABASE_ERROR, "Database error", t);
        }
    }

    @Override
    public void refresh(HttpServletRequest httpReq, HttpServletResponse httpResp) {
        try {
            String refreshPlain = cookie(httpReq, "REFRESH");
            if (refreshPlain == null || refreshPlain.isEmpty()) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Missing refresh token");
            }

            RefreshTokenRow row = rtDao.findActiveByHash(HashUtil.sha256Hex(refreshPlain));
            if (row == null || row.isRevoked() || row.expiresAt.getTime() < System.currentTimeMillis()) {
                throw new AppException(ErrorCode.FORBIDDEN, "Invalid or expired refresh token");
            }

            User u = userDao.findById(row.userId);
            if (u == null || !u.active) {
                rtDao.revokeFamily(row.userId, row.familyId);
                throw new UserInactiveException();
            }

            // rotate
            rtDao.revokeById(row.id);
            String newJti = UUID.randomUUID().toString();
            String newPlain = UUID.randomUUID().toString() + "." + newJti;
            String newHash = HashUtil.sha256Hex(newPlain);
            Timestamp newExp = new Timestamp(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000);
            rtDao.insert(
                    u.id, newJti, row.familyId, newHash, null,
                    httpReq.getRemoteAddr(), httpReq.getHeader("User-Agent"), newExp);

            long now = System.currentTimeMillis() / 1000L;
            long accessExp = now + 15 * 60;
            String access = JwtUtil.sign(u.id, u.role, u.email, accessExp);

            // CORS
            String origin = httpReq.getHeader("Origin");
            if (origin != null && origin.startsWith("http://localhost:4200")) {
                httpResp.setHeader("Access-Control-Allow-Origin", origin);
                httpResp.setHeader("Vary", "Origin");
                httpResp.setHeader("Access-Control-Allow-Credentials", "true");
            }

            // Set-Cookie
            int accessMaxAge = (int) (accessExp - now);
            httpResp.addHeader("Set-Cookie",
                    "SESSION=" + access + "; Path=/; HttpOnly; Max-Age=" + accessMaxAge + "; SameSite=None; Secure");
            httpResp.addHeader("Set-Cookie",
                    "REFRESH=" + newPlain + "; Path=/api/auth; HttpOnly; Max-Age=" + (7 * 24 * 60 * 60)
                            + "; SameSite=None; Secure");

        } catch (AppException ae) {
            throw ae;
        } catch (Throwable t) {
            throw new AppException(ErrorCode.DATABASE_ERROR, "Database error", t);
        }
    }

    @Override
    public User me(HttpServletRequest httpReq) {
        try {
            Object uidObj = httpReq.getAttribute("auth.userId");// lấy ra userId đã được AuthFilter set vào request
            if (uidObj == null)
                return null;
            return userDao.findById((Long) uidObj);
        } catch (AppException ae) {
            throw ae;
        } catch (Throwable t) {
            throw new AppException(ErrorCode.DATABASE_ERROR, "Database error", t);
        }
    }

    private String cookie(HttpServletRequest req, String name) {
        Cookie[] cs = req.getCookies();
        if (cs == null)
            return null;
        for (Cookie c : cs)
            if (name.equals(c.getName()))
                return c.getValue();
        return null;
    }
}
