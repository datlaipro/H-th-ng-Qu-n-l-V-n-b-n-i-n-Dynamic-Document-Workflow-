package com.github.datlaipro.docflow.api.web;

import com.github.datlaipro.docflow.api.auth.dto.LoginRequest;
import com.github.datlaipro.docflow.api.auth.service.AuthService;
import com.github.datlaipro.docflow.api.auth.service.AuthServiceImpl;
import com.github.datlaipro.docflow.api.auth.util.JsonUtil;
import com.google.gson.Gson;

import javax.servlet.http.*;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(LoginServlet.class.getName());

    private final AuthService auth = new AuthServiceImpl();
    private final Gson gson = new Gson();

    static class Msg { String message; Msg(String m){ this.message=m; } }

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

        try {
            LoginRequest body = null;
            String ctLower = (ct == null ? "" : ct.toLowerCase());

            if (ctLower.startsWith("application/json")) {
                // JSON: {"email":"..."} hoặc {"username":"..."}
                body = gson.fromJson(req.getReader(), LoginRequest.class);

            } else if (ctLower.startsWith("application/x-www-form-urlencoded")
                    || ctLower.startsWith("multipart/form-data")) {
                // Form: email=... & password=...  hoặc  username=... & password=...
                LoginRequest lr = new LoginRequest();
                String email = trimOrNull(req.getParameter("email"));
                String username = trimOrNull(req.getParameter("username"));
                String password = req.getParameter("password");

                lr.email = email != null ? email : username; // map username -> email nếu cần
                lr.username = username;
                lr.password = password;
                body = lr;

            } else {
                LOG.warning("[LOGIN] Unsupported Content-Type: " + ct);
                JsonUtil.json(resp, 400, new Msg("unsupported_content_type"));
                return;
            }

            // Validate đầu vào
            if (body == null) {
                JsonUtil.json(resp, 400, new Msg("missing_fields"));
                return;
            }
            if (isBlank(body.email) && isBlank(body.username)) {
                LOG.warning("[LOGIN] missing_fields (email/username)");
                JsonUtil.json(resp, 400, new Msg("missing_fields"));
                return;
            }
            if (isBlank(body.password)) {
                LOG.warning("[LOGIN] missing_fields (password)");
                JsonUtil.json(resp, 400, new Msg("missing_fields"));
                return;
            }

            // Chuẩn hoá: nếu chỉ có username thì dùng username như email đăng nhập (tuỳ logic bạn)
            if (isBlank(body.email) && !isBlank(body.username)) {
                body.email = body.username;
            }

            // Gọi service theo chữ ký cũ (LoginRequest, req, resp)
            auth.login(body, req, resp);

            // Nếu FE muốn nhận ngay SessionUser thì chỉnh AuthService trả DTO và truyền ra đây.
            JsonUtil.json(resp, 200, new Msg("ok"));

        } catch (RuntimeException re) {
            LOG.log(Level.WARNING, "[LOGIN] runtime error: " + re.getMessage(), re);
            JsonUtil.json(resp, 401, new Msg(re.getMessage()));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "[LOGIN] server_error", e);
            JsonUtil.json(resp, 500, new Msg("server_error"));
        }
    }

    private static boolean isBlank(String s){ return s==null || s.trim().isEmpty(); }
    private static String trimOrNull(String s){ return isBlank(s) ? null : s.trim(); }
}
