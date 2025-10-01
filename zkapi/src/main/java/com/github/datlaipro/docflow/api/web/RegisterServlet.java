package com.github.datlaipro.docflow.api.web;

import com.github.datlaipro.docflow.api.auth.dto.RegisterRequest;
import com.github.datlaipro.docflow.api.auth.service.AuthService;
import com.github.datlaipro.docflow.api.auth.service.AuthServiceImpl;
import com.github.datlaipro.docflow.api.auth.util.JsonUtil;
import com.google.gson.Gson;

import javax.servlet.http.*;
import javax.servlet.ServletException;
import java.io.IOException;

public class RegisterServlet extends HttpServlet {
    private final AuthService auth = new AuthServiceImpl();
    private final Gson gson = new Gson();

    static class Msg { String message; Msg(String m){ this.message = m; } }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Log request sơ bộ
        getServletContext().log("[REGISTER] POST " + req.getRequestURI() + " CT=" + req.getContentType() + " ORIGIN=" + req.getHeader("Origin"));

        // Chỉ ADMIN mới được gọi API này
        Object role = req.getAttribute("auth.role");
        if (role == null || !"ADMIN".equals(role)) {
            getServletContext().log("[REGISTER] forbidden_only_admin (role=" + role + ")");
            JsonUtil.json(resp, 403, new Msg("forbidden_only_admin"));
            return;
        }

        try {
            RegisterRequest body = gson.fromJson(req.getReader(), RegisterRequest.class);
            if (body == null || isBlank(body.email) || isBlank(body.password) || isBlank(body.fullName)) {
                getServletContext().log("[REGISTER] missing_fields (email=" + (body!=null?body.email:null) + ", fullName=" + (body!=null?body.fullName:null) + ")");
                JsonUtil.json(resp, 400, new Msg("missing_fields"));
                return;
            }

            long newId = auth.registerEmployee(body);
            getServletContext().log("[REGISTER] created_user_id_" + newId + " by role=" + role);
            JsonUtil.json(resp, 201, new Msg("created_user_id_" + newId));

        } catch (RuntimeException re) {
            // Ví dụ: email đã tồn tại, nhưng vẫn log để debug
            getServletContext().log("[REGISTER] conflict: " + re.getMessage(), re);
            re.printStackTrace(); // ra stderr -> thấy trong docker logs
            JsonUtil.json(resp, 409, new Msg(re.getMessage()));

        } catch (Exception e) {
            // Lỗi không lường trước
            getServletContext().log("[REGISTER] server_error", e);
            e.printStackTrace(); // ra stderr
            JsonUtil.json(resp, 500, new Msg("server_error"));
        }
    }

    private boolean isBlank(String s){ return s == null || s.trim().isEmpty(); }
}
