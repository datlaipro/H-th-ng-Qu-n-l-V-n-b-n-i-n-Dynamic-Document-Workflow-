package com.github.datlaipro.docflow.api.document.creat.AuthGuard;

import com.github.datlaipro.docflow.api.auth.entity.User;
import com.github.datlaipro.docflow.api.auth.service.AuthService;
import com.github.datlaipro.docflow.api.auth.service.AuthServiceImpl;
import com.github.datlaipro.docflow.api.shared.error.AppException;
import com.github.datlaipro.docflow.api.shared.error.ErrorCode;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;

import javax.servlet.http.HttpServletRequest;

/**
 * Guard xác thực/ủy quyền cho cả 2 ngữ cảnh:
 * - ZK (Executions) và
 * - Servlet thuần (ThreadLocal bind từ Servlet).
 */
public final class AuthGuard {
    private static final AuthService AUTH = new AuthServiceImpl();

    // ThreadLocal để Servlet bind request vào thread đang xử lý
    private static final ThreadLocal<HttpServletRequest> REQ_HOLDER = new ThreadLocal<>();

    private AuthGuard() {
    }

    /** Gắn request hiện tại (gọi từ Servlet trước khi dùng AuthGuard). */
    public static void bind(HttpServletRequest req) {
        REQ_HOLDER.set(req);
    }

    /** Xoá binding (gọi trong finally của Servlet để tránh leak). */
    public static void clear() {
        REQ_HOLDER.remove();
    }

    /** Lấy User đang đăng nhập; nếu không có thì ném AppException(FORBIDDEN). */
    public static User requireUser() {
        HttpServletRequest req = currentRequest();
        User u = AUTH.me(req);// AuthFilter decode JWT từ cookie/Bearer ⇒ set req.setAttribute("auth.userId",
                              // ...).AUTH.me(req) chỉ đọc attribute đó và tìm user trong DB.
        if (u == null) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn phải đăng nhập mới được thực hiện thao tác này");
        }
        if (!u.active) {
            throw new AppException(ErrorCode.FORBIDDEN, "Tài khoản đang bị khóa/không hoạt động");
        }
        return u;
    }

    /** Lấy userId đang đăng nhập (tiện dùng), sẽ ném lỗi nếu chưa đăng nhập. */
    public static long requireUserId() {
        return requireUser().id;
    }

    /** (Tuỳ chọn) Ràng buộc vai trò. */
    public static void requireRole(String... roles) {
        User u = requireUser();
        if (u.role == null) {
            throw new AppException(ErrorCode.FORBIDDEN, "Thiếu vai trò người dùng");
        }
        for (String r : roles) {
            if (u.role.equals(r))
                return;
        }
        throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này");
    }

    // --- helpers ---
    private static HttpServletRequest currentRequest() {
        // 1) Ưu tiên request do Servlet bind vào ThreadLocal
        HttpServletRequest req = REQ_HOLDER.get();
        if (req != null)
            return req;

        // 2) Fallback cho ngữ cảnh ZK
        Execution ex = Executions.getCurrent();
        if (ex != null) {
            Object nativeReq = ex.getNativeRequest();
            if (nativeReq instanceof HttpServletRequest) {
                return (HttpServletRequest) nativeReq;
            }
            throw new AppException(ErrorCode.FORBIDDEN, "Invalid request");
        }

        // 3) Không có ngữ cảnh hợp lệ
        throw new AppException(ErrorCode.FORBIDDEN, "No request context");
    }
}
