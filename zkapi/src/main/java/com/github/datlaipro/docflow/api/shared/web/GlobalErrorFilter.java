package com.github.datlaipro.docflow.api.shared.web;

import com.github.datlaipro.docflow.api.auth.util.JsonUtil;
import com.github.datlaipro.docflow.api.shared.error.AppException;
import com.github.datlaipro.docflow.api.shared.error.ErrorCode;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GlobalErrorFilter implements Filter {
    private static final Logger LOG = Logger.getLogger(GlobalErrorFilter.class.getName());

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // no-op
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        final HttpServletResponse resp = (HttpServletResponse) response;

        try {
            chain.doFilter(request, response);

        } catch (AppException ae) {
            // Ví dụ: chưa đăng nhập/không đủ quyền/limit...
            LOG.log(Level.INFO, "[APP_ERROR] " + ae.getCode() + " " + ae.getMessage(), ae);
            if (!resp.isCommitted()) {
                resp.reset();
                if (ae.getCode() == ErrorCode.TOO_MANY_ATTEMPTS && ae.getRetryAfterSeconds() != null) {
                    resp.setHeader("Retry-After", String.valueOf(ae.getRetryAfterSeconds()));
                }
                // ĐẢM BẢO set status đúng (401/403/429... tuỳ ae.getHttpStatus())
                JsonUtil.json(resp, ae.getHttpStatus(),
                        new Msg(ae.getCode().name().toLowerCase(), ae.getMessage()));
            }
            return; // RẤT QUAN TRỌNG: dừng tại đây, không chạy tiếp

        } catch (IllegalArgumentException iae) {
            LOG.log(Level.WARNING, "[BAD_REQUEST] " + iae.getMessage(), iae);
            if (!resp.isCommitted()) {
                resp.reset();
                JsonUtil.json(resp, HttpServletResponse.SC_BAD_REQUEST,
                        new Msg("bad_request", iae.getMessage()));
            }
            return;

        } catch (Throwable e) {
            // Phân loại lỗi chung
            Throwable root = rootCause(e);
            LOG.log(Level.SEVERE, "[UNHANDLED] " + root.getClass().getSimpleName() + ": " + root.getMessage(), e);
            int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            String code = "internal_error";
            if (root instanceof SQLException) {
                code = "db_error";
            }
            if (!resp.isCommitted()) {
                resp.reset();
                JsonUtil.json(resp, status, new Msg(code, "Internal error"));
            }
            return;
        }
    }

    @Override
    public void destroy() {
        // no-op
    }

    private static Throwable rootCause(Throwable t) {
        Throwable r = t;
        while (r.getCause() != null && r.getCause() != r) r = r.getCause();
        return r;
    }

    // POJO thông điệp lỗi
    public static class Msg {
        public String code;
        public String message;
        public Msg(String code, String message) { this.code = code; this.message = message; }
    }
}
