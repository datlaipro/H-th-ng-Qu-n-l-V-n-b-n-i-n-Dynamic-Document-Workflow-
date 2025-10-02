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
            // không lộ inactive khác invalid để tránh dò tài khoản
            if (u == null)
                throw new InvalidCredentialsException();
            if (!u.active)
                throw new UserInactiveException();

            String hash = userDao.findPasswordHashByEmail(req.email);
            if (hash == null || !PasswordUtil.verify(req.password, hash)) {
                throw new InvalidCredentialsException();
            }

            long now = System.currentTimeMillis() / 1000L;
            long accessExp = now + 15 * 60; // 15m
            String access = JwtUtil.sign(u.id, u.role, u.email, accessExp);

            String familyId = UUID.randomUUID().toString();
            String jti = UUID.randomUUID().toString();
            String refreshPlain = UUID.randomUUID().toString() + "." + jti;
            String refreshHash = HashUtil.sha256Hex(refreshPlain);
            Timestamp rtExp = new Timestamp(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000);

            rtDao.insert(
                    u.id, jti, familyId, refreshHash, null,
                    httpReq.getRemoteAddr(), httpReq.getHeader("User-Agent"), rtExp);

            Cookie c1 = new Cookie("SESSION", access);
            c1.setHttpOnly(true);
            c1.setPath("/");
            c1.setMaxAge((int) (accessExp - now));
            // c1.setSecure(true);
            httpResp.addCookie(c1);
            httpResp.setHeader("Set-Cookie", "SESSION=" + access + "; Path=/; HttpOnly; SameSite=Lax");

            Cookie c2 = new Cookie("REFRESH", refreshPlain);
            c2.setHttpOnly(true);
            c2.setPath("/api/auth");
            c2.setMaxAge(7 * 24 * 60 * 60);
            // c2.setSecure(true);
            httpResp.addCookie(c2);
            httpResp.addHeader("Set-Cookie", "REFRESH=" + refreshPlain + "; Path=/api/auth; HttpOnly; SameSite=Strict");

        } catch (AppException ae) {
            throw ae; // để GlobalErrorFilter trả JSON chuẩn
        } catch (Throwable t) {
            // DB/driver hoặc lỗi bất ngờ khác
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
            // clear cookies
            Cookie c1 = new Cookie("SESSION", "");
            c1.setHttpOnly(true);
            c1.setPath("/");
            c1.setMaxAge(0);
            Cookie c2 = new Cookie("REFRESH", "");
            c2.setHttpOnly(true);
            c2.setPath("/api/auth");
            c2.setMaxAge(0);
            httpResp.addCookie(c1);
            httpResp.addCookie(c2);
            httpResp.setHeader("Set-Cookie", "SESSION=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
            httpResp.addHeader("Set-Cookie", "REFRESH=; Path=/api/auth; Max-Age=0; HttpOnly; SameSite=Strict");

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
                // thiếu refresh cookie -> coi như bad request
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Missing refresh token");
            }

            RefreshTokenRow row = rtDao.findActiveByHash(HashUtil.sha256Hex(refreshPlain));
            if (row == null || row.isRevoked() || row.expiresAt.getTime() < System.currentTimeMillis()) {
                // refresh token không hợp lệ/hết hạn
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

            Cookie c1 = new Cookie("SESSION", access);
            c1.setHttpOnly(true);
            c1.setPath("/");
            c1.setMaxAge((int) (accessExp - now));
            httpResp.addCookie(c1);
            httpResp.setHeader("Set-Cookie", "SESSION=" + access + "; Path=/; HttpOnly; SameSite=Lax");

            Cookie c2 = new Cookie("REFRESH", newPlain);
            c2.setHttpOnly(true);
            c2.setPath("/api/auth");
            c2.setMaxAge(7 * 24 * 60 * 60);
            httpResp.addCookie(c2);
            httpResp.addHeader("Set-Cookie", "REFRESH=" + newPlain + "; Path=/api/auth; HttpOnly; SameSite=Strict");

        } catch (AppException ae) {
            throw ae;
        } catch (Throwable t) {
            throw new AppException(ErrorCode.DATABASE_ERROR, "Database error", t);
        }
    }

    @Override
    public User me(HttpServletRequest httpReq) {
        try {
            Object uidObj = httpReq.getAttribute("auth.userId");
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
