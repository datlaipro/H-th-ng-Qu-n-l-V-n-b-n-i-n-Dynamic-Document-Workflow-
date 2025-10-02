package com.github.datlaipro.docflow.api.web;

import com.github.datlaipro.docflow.api.auth.dto.LoginRequest;
import com.github.datlaipro.docflow.api.auth.service.AuthService;
import com.github.datlaipro.docflow.api.auth.service.AuthServiceImpl;
import com.github.datlaipro.docflow.api.auth.util.JsonUtil;
import com.github.datlaipro.docflow.api.shared.error.AppException;
import com.github.datlaipro.docflow.api.shared.error.ErrorCode;
import com.google.gson.Gson;

import javax.servlet.http.*;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Logger;

public class LoginServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(LoginServlet.class.getName());

    private final AuthService auth = new AuthServiceImpl();
    private final Gson gson = new Gson();

    static class Msg { String code; String message; Msg(String c,String m){code=c;message=m;} }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        final String ct = req.getContentType();
        LOG.info("[LOGIN] " + req.getMethod() + " " + req.getRequestURI()
                + " CT=" + ct + " ORIGIN=" + req.getHeader("Origin"));

        // ---- Parse body theo Content-Type ----
        LoginRequest body = null;
        String ctLower = (ct == null ? "" : ct.toLowerCase(Locale.ROOT));

        if (ctLower.startsWith("application/json")) {
            body = gson.fromJson(req.getReader(), LoginRequest.class);

        } else if (ctLower.startsWith("application/x-www-form-urlencoded")
                || ctLower.startsWith("multipart/form-data")) {
            LoginRequest lr = new LoginRequest();
            String email = trimOrNull(req.getParameter("email"));
            String username = trimOrNull(req.getParameter("username"));
            String password = req.getParameter("password");
            lr.email = (email != null ? email : username);
            lr.username = username;
            lr.password = password;
            body = lr;

        } else {
            // Không định nghĩa riêng 415 trong ErrorCode → quy về VALIDATION_ERROR
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Unsupported Content-Type");
        }

        // ---- Validate đầu vào (ném AppException để GlobalErrorFilter xử lý) ----
        if (body == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Missing request body");
        }
        if (isBlank(body.email) && isBlank(body.username)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Email/username is required");
        }
        if (isBlank(body.password)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Password is required");
        }

        // Chuẩn hoá: nếu chỉ có username thì dùng username như email đăng nhập (tuỳ logic)
        if (isBlank(body.email) && !isBlank(body.username)) {
            body.email = body.username;
        }

        // ---- Gọi service: nếu có lỗi nghiệp vụ, service sẽ ném AppException chuyên biệt ----
        auth.login(body, req, resp);

        // ---- Thành công ----
        JsonUtil.json(resp, 200, new Msg("ok", "Login success"));
    }

    // Helpers
    private static boolean isBlank(String s){ return s==null || s.trim().isEmpty(); }
    private static String trimOrNull(String s){ return isBlank(s) ? null : s.trim(); }
}