package com.github.datlaipro.docflow.api.web;

import com.github.datlaipro.docflow.api.auth.util.JwtUtil;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class AuthFilter implements Filter {
    private static final Logger LOG = Logger.getLogger(AuthFilter.class.getName());

    // Các đường public (không cần đăng nhập)
    private static final Set<String> PUBLIC_PREFIXES = new HashSet<>(Arrays.asList(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh",
        "/api/health",
        "/zkau" // nếu ZK ajax cần public
    ));

    @Override public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // Cho phép preflight
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        final String path = req.getRequestURI();

        // Nếu endpoint public → bỏ qua kiểm tra
        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Đọc token từ cookie SESSION hoặc Bearer
        String token = readCookie(req, "SESSION");
        if (token == null || token.isEmpty()) {
            String authz = req.getHeader("Authorization");
            if (authz != null && authz.startsWith("Bearer ")) token = authz.substring(7).trim();
        }

        if (token == null || token.isEmpty()) {
            LOG.info("[AUTH] No token (cookie or Bearer) on " + req.getMethod() + " " + path);
            unauthorized(resp, req);
            return; // QUAN TRỌNG: dừng tại đây
        }

        // Có token → verify
        try {
            Map<String, Object> payload = JwtUtil.verify(token); // phải verify signature thật sự
            if (payload == null) {
                LOG.warning("[AUTH] verify() returned null payload, uri=" + path);
                unauthorized(resp, req);
                return;
            }
            Long userId = coerceLong(payload.get("sub"));
            String role  = coerceString(payload.get("role"));
            String email = coerceString(payload.get("email"));
            if (userId == null) {
                LOG.warning("[AUTH] Missing/invalid 'sub' claim, uri=" + path);
                unauthorized(resp, req);
                return;
            }

            // OK → gắn vào request để downstream dùng
            req.setAttribute("auth.userId", userId);
            if (role != null)  req.setAttribute("auth.role", role);
            if (email != null) req.setAttribute("auth.email", email);
            LOG.info("[AUTH] OK userId=" + userId + " role=" + role + " uri=" + path);

            chain.doFilter(request, response);
        } catch (Exception e) {
            LOG.warning("[AUTH] invalid SESSION token: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            unauthorized(resp, req);
        }
    }

    @Override public void destroy() {}

    // ---- helpers ----

    private static boolean isPublic(String uri) {
        if (uri == null) return true;
        for (String p : PUBLIC_PREFIXES) {
            if (uri.startsWith(p)) return true;
        }
        // ví dụ: cho GET file tĩnh public
        if (uri.startsWith("/assets/") || uri.startsWith("/static/")) return true;
        return false;
    }

    private static void unauthorized(HttpServletResponse resp, HttpServletRequest req) throws IOException {
        // Nếu bạn muốn CORS hoạt động khi trả lỗi trực tiếp từ filter (browser), có thể echo Origin:
        String origin = req.getHeader("Origin");
        if (origin != null) {
            resp.setHeader("Access-Control-Allow-Origin", origin);
            resp.setHeader("Vary", "Origin");
            resp.setHeader("Access-Control-Allow-Credentials", "true");
        }

        resp.reset(); // xoá mọi thứ đã ghi (nếu có)
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");
        resp.getWriter().write("{\"error\":\"Bạn phải đăng nhập\"}");
    }

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
    private static String coerceString(Object v) { return v == null ? null : String.valueOf(v); }
}
