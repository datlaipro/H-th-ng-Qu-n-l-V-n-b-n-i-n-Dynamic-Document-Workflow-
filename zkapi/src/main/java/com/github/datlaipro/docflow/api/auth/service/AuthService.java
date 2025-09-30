package com.github.datlaipro.docflow.api.auth.service;

import com.github.datlaipro.docflow.api.auth.dto.LoginRequest;
import com.github.datlaipro.docflow.api.auth.dto.RegisterRequest;
import com.github.datlaipro.docflow.api.auth.entity.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface AuthService {
    /** Xác thực, set cookie SESSION + REFRESH nếu ok */
    void login(LoginRequest req, HttpServletRequest httpReq, HttpServletResponse httpResp) throws Exception;

    /** Chỉ ADMIN mới được tạo EMPLOYEE */
    long registerEmployee(RegisterRequest req) throws Exception;

    /** Thu hồi refresh hiện tại (nếu có) + xoá cookie */
    void logout(HttpServletRequest httpReq, HttpServletResponse httpResp) throws Exception;

    /** Dựa vào cookie REFRESH để cấp SESSION mới + rotate refresh */
    void refresh(HttpServletRequest httpReq, HttpServletResponse httpResp) throws Exception;

    /** Lấy user hiện tại từ cookie SESSION (đã verify ở Filter) */
    User me(HttpServletRequest httpReq) throws Exception;
}
