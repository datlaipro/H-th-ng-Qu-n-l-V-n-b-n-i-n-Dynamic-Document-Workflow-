package com.github.datlaipro.docflow.api.auth.service;

import com.github.datlaipro.docflow.api.auth.dto.LoginRequest;
import com.github.datlaipro.docflow.api.auth.dto.RegisterRequest;
import com.github.datlaipro.docflow.api.auth.entity.User;
import com.github.datlaipro.docflow.api.auth.repo.*;
import com.github.datlaipro.docflow.api.auth.util.*;

import javax.servlet.http.*;
import java.sql.Timestamp;
import java.util.UUID;

public class AuthServiceImpl implements AuthService {
    private final UserDao userDao = new UserDaoImpl();
    private final RefreshTokenDao rtDao = new RefreshTokenDaoImpl();

    @Override
    public void login(LoginRequest req, HttpServletRequest httpReq, HttpServletResponse httpResp) throws Exception {
        User u = userDao.findByEmail(req.email);
        if (u == null || !u.active) throw new RuntimeException("invalid_credentials");
        String hash = userDao.findPasswordHashByEmail(req.email);
        if (hash == null || !PasswordUtil.verify(req.password, hash)) throw new RuntimeException("invalid_credentials");

        long now = System.currentTimeMillis()/1000L;
        long accessExp = now + 15*60; // 15m
        String access = JwtUtil.sign(u.id, u.role, u.email, accessExp);

        String familyId = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();
        String refreshPlain = UUID.randomUUID().toString() + "." + jti;
        String refreshHash  = HashUtil.sha256Hex(refreshPlain);
        Timestamp rtExp = new Timestamp(System.currentTimeMillis() + 7L*24*60*60*1000);

        rtDao.insert(u.id, jti, familyId, refreshHash, null, httpReq.getRemoteAddr(), httpReq.getHeader("User-Agent"), rtExp);

        Cookie c1 = new Cookie("SESSION", access);
        c1.setHttpOnly(true); c1.setPath("/"); c1.setMaxAge((int)(accessExp - now));
        // c1.setSecure(true);
        httpResp.addCookie(c1);
        httpResp.setHeader("Set-Cookie", "SESSION="+access+"; Path=/; HttpOnly; SameSite=Lax");

        Cookie c2 = new Cookie("REFRESH", refreshPlain);
        c2.setHttpOnly(true); c2.setPath("/api/auth"); c2.setMaxAge(7*24*60*60);
        // c2.setSecure(true);
        httpResp.addCookie(c2);
        httpResp.addHeader("Set-Cookie", "REFRESH="+refreshPlain+"; Path=/api/auth; HttpOnly; SameSite=Strict");
    }

    @Override
    public long registerEmployee(RegisterRequest req) throws Exception {
        String hash = PasswordUtil.hash(req.password);
        return userDao.createEmployee(req.email.trim(), req.fullName.trim(), hash);
    }

    @Override
    public void logout(HttpServletRequest httpReq, HttpServletResponse httpResp) throws Exception {
        String refreshPlain = cookie(httpReq, "REFRESH");
        if (refreshPlain != null && !refreshPlain.isEmpty()) {
            RefreshTokenRow row = rtDao.findActiveByHash(HashUtil.sha256Hex(refreshPlain));
            if (row != null && !row.isRevoked()) rtDao.revokeById(row.id);
        }
        // clear cookies
        Cookie c1 = new Cookie("SESSION", ""); c1.setHttpOnly(true); c1.setPath("/"); c1.setMaxAge(0);
        Cookie c2 = new Cookie("REFRESH", ""); c2.setHttpOnly(true); c2.setPath("/api/auth"); c2.setMaxAge(0);
        httpResp.addCookie(c1); httpResp.addCookie(c2);
        httpResp.setHeader("Set-Cookie","SESSION=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        httpResp.addHeader("Set-Cookie","REFRESH=; Path=/api/auth; Max-Age=0; HttpOnly; SameSite=Strict");
    }

    @Override
    public void refresh(HttpServletRequest httpReq, HttpServletResponse httpResp) throws Exception {
        String refreshPlain = cookie(httpReq, "REFRESH");
        if (refreshPlain == null || refreshPlain.isEmpty()) throw new RuntimeException("no_refresh");
        RefreshTokenRow row = rtDao.findActiveByHash(HashUtil.sha256Hex(refreshPlain));
        if (row == null || row.isRevoked() || row.expiresAt.getTime() < System.currentTimeMillis())
            throw new RuntimeException("invalid_or_expired");

        User u = userDao.findById(row.userId);
        if (u == null || !u.active) {
            rtDao.revokeFamily(row.userId, row.familyId);
            throw new RuntimeException("user_inactive");
        }

        // rotate
        rtDao.revokeById(row.id);
        String newJti = UUID.randomUUID().toString();
        String newPlain = UUID.randomUUID().toString() + "." + newJti;
        String newHash = HashUtil.sha256Hex(newPlain);
        Timestamp newExp = new Timestamp(System.currentTimeMillis() + 7L*24*60*60*1000);
        rtDao.insert(u.id, newJti, row.familyId, newHash, null, httpReq.getRemoteAddr(), httpReq.getHeader("User-Agent"), newExp);

        long now = System.currentTimeMillis()/1000L;
        long accessExp = now + 15*60;
        String access = JwtUtil.sign(u.id, u.role, u.email, accessExp);

        Cookie c1 = new Cookie("SESSION", access);
        c1.setHttpOnly(true); c1.setPath("/"); c1.setMaxAge((int)(accessExp - now));
        httpResp.addCookie(c1);
        httpResp.setHeader("Set-Cookie", "SESSION="+access+"; Path=/; HttpOnly; SameSite=Lax");

        Cookie c2 = new Cookie("REFRESH", newPlain);
        c2.setHttpOnly(true); c2.setPath("/api/auth"); c2.setMaxAge(7*24*60*60);
        httpResp.addCookie(c2);
        httpResp.addHeader("Set-Cookie", "REFRESH="+newPlain+"; Path=/api/auth; HttpOnly; SameSite=Strict");
    }

    @Override
    public User me(HttpServletRequest httpReq) throws Exception {
        Object uidObj = httpReq.getAttribute("auth.userId");
        if (uidObj == null) return null;
        return userDao.findById((Long) uidObj);
    }

    private String cookie(HttpServletRequest req, String name) {
        Cookie[] cs = req.getCookies();
        if (cs == null) return null;
        for (Cookie c : cs) if (name.equals(c.getName())) return c.getValue();
        return null;
    }
}
