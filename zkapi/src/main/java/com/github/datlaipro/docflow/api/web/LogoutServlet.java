package com.github.datlaipro.docflow.api.web;

import com.github.datlaipro.docflow.api.auth.service.AuthService;
import com.github.datlaipro.docflow.api.auth.service.AuthServiceImpl;
import com.github.datlaipro.docflow.api.auth.util.JsonUtil;

import javax.servlet.http.*;
import javax.servlet.ServletException;
import java.io.IOException;

public class LogoutServlet extends HttpServlet {
    private final AuthService auth = new AuthServiceImpl();
    static class Msg { String message; Msg(String m){ this.message = m; } }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            auth.logout(req, resp);
            JsonUtil.json(resp, 200, new Msg("logged_out"));
        } catch (Exception e) {
            JsonUtil.json(resp, 500, new Msg("server_error"));
        }
    }
}
