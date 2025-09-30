package com.github.datlaipro.docflow.api.web;

import com.github.datlaipro.docflow.api.auth.entity.User;
import com.github.datlaipro.docflow.api.auth.service.AuthService;
import com.github.datlaipro.docflow.api.auth.service.AuthServiceImpl;
import com.github.datlaipro.docflow.api.auth.util.JsonUtil;

import javax.servlet.http.*;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MeServlet extends HttpServlet {
    private final AuthService auth = new AuthServiceImpl();
    static class Msg { String message; Msg(String m){ this.message = m; } }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            User u = auth.me(req);
            if (u == null) {
                JsonUtil.json(resp, 401, new Msg("unauthorized"));
                return;
            }
            Map<String,Object> body = new HashMap<>();
            body.put("userId", u.id);
            body.put("email",  u.email);
            body.put("role",   u.role);
            JsonUtil.json(resp, 200, body);
        } catch (Exception e) {
            JsonUtil.json(resp, 500, new Msg("server_error"));
        }
    }
}
