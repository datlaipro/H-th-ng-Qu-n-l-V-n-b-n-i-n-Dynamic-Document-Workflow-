package com.github.datlaipro.docflow.api.web;

import com.github.datlaipro.docflow.api.auth.util.JwtUtil;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

public class AuthFilter implements Filter {
    private static final Logger LOG = Logger.getLogger(AuthFilter.class.getName());

    @Override public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest req = (HttpServletRequest) request;

        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String token = readCookie(req, "SESSION");
        if (token == null || token.isEmpty()) {
            String authz = req.getHeader("Authorization");
            if (authz != null && authz.startsWith("Bearer ")) {
                token = authz.substring(7).trim();
            }
        }

        if (token != null && !token.isEmpty()) {
            try {
                // JwtUtil.verify nên THỰC SỰ verify signature và trả claim map
                Map<String, Object> payload = JwtUtil.verify(token);
                if (payload != null) {
                    Long userId = coerceLong(payload.get("sub"));// 'sub' claim là userId
                    String role  = coerceString(payload.get("role"));
                    String email = coerceString(payload.get("email"));
                    if (userId != null) {
                        req.setAttribute("auth.userId", userId);
                        if (role != null)  req.setAttribute("auth.role", role);
                        if (email != null) req.setAttribute("auth.email", email);
                        LOG.info("[AUTH] OK userId=" + userId + " role=" + role + " uri=" + req.getRequestURI());
                    } else {
                        LOG.warning("[AUTH] Missing/invalid 'sub' claim, uri=" + req.getRequestURI());
                    }
                } else {
                    LOG.warning("[AUTH] verify() returned null payload, uri=" + req.getRequestURI());
                }
            } catch (Exception e) {
                LOG.warning("[AUTH] invalid SESSION token: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        } else {
            LOG.info("[AUTH] No token (cookie or Bearer) on " + req.getMethod() + " " + req.getRequestURI());
        }

        chain.doFilter(request, response);
    }

    @Override public void destroy() {}

    // -------- helpers --------
    private static String readCookie(HttpServletRequest req, String name) {
        Cookie[] cs = req.getCookies();
        if (cs == null) return null;
        for (Cookie c : cs) if (name.equals(c.getName())) return c.getValue();
        return null;
    }

    private static Long coerceLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long)    return (Long) v;
        if (v instanceof Integer) return ((Integer) v).longValue();
        if (v instanceof Double)  return ((Double) v).longValue();
        if (v instanceof Float)   return ((Float) v).longValue();
        if (v instanceof String) {
            try { return Long.parseLong(((String) v).trim()); } catch (Exception ignored) {}
        }
        return null;
    }

    private static String coerceString(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
