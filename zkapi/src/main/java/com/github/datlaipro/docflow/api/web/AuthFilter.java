package com.github.datlaipro.docflow.api.web;

import com.github.datlaipro.docflow.api.auth.util.JwtUtil;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Map;

public class AuthFilter implements Filter {

    @Override public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest)) { chain.doFilter(request, response); return; }
        HttpServletRequest req = (HttpServletRequest) request;

        // Đọc cookie SESSION
        String token = null;
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("SESSION".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }

        if (token != null && !token.isEmpty()) {
            try {
                Map<String,Object> payload = JwtUtil.verify(token);
                if (payload != null) {
                    // Gắn vào attribute để servlet dùng
                    req.setAttribute("auth.userId", ((Double)payload.get("sub")).longValue());
                    req.setAttribute("auth.role", (String)payload.get("role"));
                    req.setAttribute("auth.email", (String)payload.get("email"));
                }
            } catch (Exception ignored) {}
        }

        chain.doFilter(request, response);
    }

    @Override public void destroy() {}
}
