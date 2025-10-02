package com.github.datlaipro.docflow.api.shared.web;

import com.github.datlaipro.docflow.api.auth.util.JsonUtil;
import com.github.datlaipro.docflow.api.shared.error.AppException;
import com.github.datlaipro.docflow.api.shared.error.ErrorCode;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GlobalErrorFilter implements Filter {
    private static final Logger LOG = Logger.getLogger(GlobalErrorFilter.class.getName());

    static class Msg {
        public String code;
        public String message;
        Msg(String c, String m){ code = c; message = m; }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // no-op
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse resp = (HttpServletResponse) response;

        try {
            chain.doFilter(request, response);

        } catch (AppException ae) {
            LOG.log(Level.INFO, "[APP_ERROR] " + ae.getCode() + " " + ae.getMessage(), ae);
            if (ae.getCode() == ErrorCode.TOO_MANY_ATTEMPTS && ae.getRetryAfterSeconds() != null) {
                resp.setHeader("Retry-After", String.valueOf(ae.getRetryAfterSeconds()));
            }
            JsonUtil.json(resp, ae.getHttpStatus(),
                    new Msg(ae.getCode().name().toLowerCase(), ae.getMessage()));

        } catch (Exception e) {
            // Phân loại trong khối catch chung
            Throwable root = rootCause(e);
            if (root instanceof SQLException) {
                LOG.log(Level.SEVERE, "[DB_ERROR] " + root.getMessage(), e);
                JsonUtil.json(resp, ErrorCode.DATABASE_ERROR.httpStatus,
                        new Msg("database_error", "Database error"));
            } else {
                LOG.log(Level.SEVERE, "[SERVER_ERROR] " + e.getMessage(), e);
                JsonUtil.json(resp, ErrorCode.SERVER_ERROR.httpStatus,
                        new Msg("server_error", "Unexpected server error"));
            }
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
}
