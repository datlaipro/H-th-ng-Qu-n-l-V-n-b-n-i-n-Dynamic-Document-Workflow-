package com.github.datlaipro.docflow.api.web;

import com.github.datlaipro.docflow.api.auth.service.AuthService;
import com.github.datlaipro.docflow.api.auth.service.AuthServiceImpl;
import com.github.datlaipro.docflow.api.auth.util.JsonUtil;

import javax.servlet.http.*;
import javax.servlet.ServletException;
import java.io.IOException;

public class RefreshServlet extends HttpServlet {
    private final AuthService auth = new AuthServiceImpl();
    static class Msg { String message; Msg(String m){ this.message = m; } }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            auth.refresh(req, resp);
            JsonUtil.json(resp, 200, new Msg("ok"));
        } catch (RuntimeException re) {
            // ví dụ: không có REFRESH, token hết hạn, revoked, v.v.
            JsonUtil.json(resp, 401, new Msg(re.getMessage()));
        } catch (Exception e) {
            JsonUtil.json(resp, 500, new Msg("server_error"));
        }
    }
}
