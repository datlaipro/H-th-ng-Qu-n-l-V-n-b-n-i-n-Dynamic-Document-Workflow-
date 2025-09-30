package com.github.datlaipro.docflow.api.auth.util;


import com.google.gson.Gson;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

public class JsonUtil {
    private static final Gson gson = new Gson();

    public static void json(HttpServletResponse resp, int status, Object body) {
        resp.setStatus(status);
        resp.setContentType("application/json; charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.print(gson.toJson(body));
        } catch (Exception ignored) {}
    }
}
