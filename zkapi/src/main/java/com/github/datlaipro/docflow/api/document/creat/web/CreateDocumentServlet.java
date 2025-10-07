package com.github.datlaipro.docflow.api.document.creat.web;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.github.datlaipro.docflow.api.document.creat.AuthGuard.AuthGuard;
import com.github.datlaipro.docflow.api.document.creat.dto.DocumentReqIN;
import com.github.datlaipro.docflow.api.document.creat.dto.DocumentReqOUT;
import com.github.datlaipro.docflow.api.document.creat.error.DocumentDuplicateException;
import com.github.datlaipro.docflow.api.document.creat.service.DocumentCreateService;
import com.github.datlaipro.docflow.api.document.creat.service.DocumentCreateServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * POST /api/documents
 * Payload hợp lệ:
 * - INBOUND: DocumentReqIN
 * - OUTBOUND: DocumentReqOUT
 *
 * QUY TẮC TYPE:
 * - Lấy từ body trước (field "type"), sau đó mới đến query/header ("type" hoặc "X-Doc-Type").
 * - KHÔNG autodetect theo trường dữ liệu.
 * - Chỉ chấp nhận INBOUND | OUTBOUND.
 */
@WebServlet(name = "CreateDocumentServlet", urlPatterns = { "/api/documents" })
public class CreateDocumentServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(CreateDocumentServlet.class.getName());

    private final DocumentCreateService service = new DocumentCreateServiceImpl();

    private final ObjectMapper om = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setCors(resp, req.getHeader("Origin"));
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        final long t0 = System.currentTimeMillis();
        setCors(resp, req.getHeader("Origin"));
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        final String method = req.getMethod();
        final String uri = req.getRequestURI();
        final String ct = req.getContentType();
        final String origin = req.getHeader("Origin");

        PrintWriter out = resp.getWriter();
        LOG.info("[DOC][HTTP][REQ] " + method + " " + uri + " CT=" + ct + " ORIGIN=" + origin);

        // bind request vào AuthGuard để service có context
        AuthGuard.bind(req);
        try {
            // Parse JSON body
            JsonNode root = om.readTree(req.getInputStream());
            if (root == null || root.isNull()) {
                LOG.warning("[DOC][HTTP][ERR] Empty body");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("{\"error\":\"Empty body\"}");
                return;
            }

            // Preview body an toàn (cắt ngắn, chỉ vài trường)
            LOG.fine("[DOC][HTTP][BODY] " + safePreviewJson(root, 400));

            // --- Đọc type ---
            String bodyType = null;
            if (root.hasNonNull("type")) {
                bodyType = root.get("type").asText(null);
                if (bodyType != null) bodyType = bodyType.trim().toUpperCase();
            }

            String qhType = readType(req); // đã upper-case & validated trong hàm này (trả null nếu không hợp lệ)
            if (qhType != null) qhType = qhType.trim().toUpperCase();

            // Quy tắc: body > query/header; không autodetect
            final String resolvedType = (bodyType != null) ? bodyType : qhType;

            // Validate type hợp lệ
            if (!"INBOUND".equals(resolvedType) && !"OUTBOUND".equals(resolvedType)) {
                LOG.warning("[DOC][HTTP][ERR] Missing/invalid type (got=" + resolvedType + ")");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("{\"error\":\"Thiếu hoặc sai 'type' (INBOUND|OUTBOUND).\"}");
                return;
            }

            // Nếu body và query/header cùng có thì phải trùng nhau
            if (bodyType != null && qhType != null && !bodyType.equals(qhType)) {
                LOG.warning("[DOC][HTTP][ERR] 'type' ở body và query/header không trùng nhau. body=" + bodyType + ", qh=" + qhType);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("{\"error\":\"'type' ở body và query/header không trùng nhau.\"}");
                return;
            }

            LOG.info("[DOC][HTTP] resolvedType=" + resolvedType);

            long id;
            switch (resolvedType) {
                case "INBOUND": {
                    DocumentReqIN body = om.treeToValue(root, DocumentReqIN.class);
                    final String number = body.number; // dùng để log/hiển thị khi duplicate
                    LOG.fine("[DOC][INBOUND] number=" + safe(number)
                            + " receivedAt=" + safe(body.receivedAt)
                            + " senderUnit=" + safe(body.senderUnit)
                            + " attCount=" + (body.attachments == null ? 0 : body.attachments.size()));
                    try {
                        id = service.createInbound(body);
                    } catch (DocumentDuplicateException dup) {
                        LOG.warning("[DOC][INBOUND][DUP] number=" + safe(number) + " msg=" + dup.getMessage());
                        resp.setStatus(HttpServletResponse.SC_CONFLICT); // 409
                        out.write("{\"error\":\"Số hiệu văn bản đã tồn tại\",\"type\":\"INBOUND\",\"number\":\"" + escape(number) + "\"}");
                        return;
                    }
                    break;
                }
                case "OUTBOUND": {
                    DocumentReqOUT body = om.treeToValue(root, DocumentReqOUT.class);
                    final String number = body.number; // dùng để log/hiển thị khi duplicate
                    LOG.fine("[DOC][OUTBOUND] number=" + safe(number)
                            + " issuedAt=" + safe(body.issuedAt)
                            + " recipientUnit=" + safe(body.recipientUnit)
                            + " attCount=" + (body.attachments == null ? 0 : body.attachments.size()));
                    try {
                        id = service.createOutbound(body);
                    } catch (DocumentDuplicateException dup) {
                        LOG.warning("[DOC][OUTBOUND][DUP] number=" + safe(number) + " msg=" + dup.getMessage());
                        resp.setStatus(HttpServletResponse.SC_CONFLICT); // 409
                        out.write("{\"error\":\"Số hiệu văn bản đã tồn tại\",\"type\":\"OUTBOUND\",\"number\":\"" + escape(number) + "\"}");
                        return;
                    }
                    break;
                }
                default: {
                    LOG.warning("[DOC][HTTP][ERR] Invalid type value: " + resolvedType);
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.write("{\"error\":\"Invalid type. Must be INBOUND or OUTBOUND\"}");
                    return;
                }
            }

            long dt = System.currentTimeMillis() - t0;
            LOG.info("[DOC][HTTP][OK] id=" + id + " type=" + resolvedType + " in " + dt + " ms");

            resp.setStatus(HttpServletResponse.SC_OK);
            out.write("{\"id\":" + id + ",\"type\":\"" + resolvedType + "\",\"message\":\"created\"}");
        } catch (IllegalArgumentException ex) {
            LOG.log(Level.WARNING, "[DOC][HTTP][BAD_REQUEST] " + ex.getMessage(), ex);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"" + escape(ex.getMessage()) + "\"}");
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "[DOC][HTTP][EX] " + ex.getMessage(), ex);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"Internal error: " + escape(ex.getMessage()) + "\"}");
        } finally {
            // luôn clear để tránh leak ThreadLocal giữa các request
            AuthGuard.clear();
        }
    }

    // ===== Helpers =====

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void setCors(HttpServletResponse resp, String origin) {
        if (origin != null && origin.startsWith("http://localhost:4200")) {
            resp.setHeader("Access-Control-Allow-Origin", origin);
            resp.setHeader("Vary", "Origin");
            resp.setHeader("Access-Control-Allow-Credentials", "true");
            resp.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
            resp.setHeader("Access-Control-Allow-Headers",
                    "Origin,Accept,X-Requested-With,Content-Type,Authorization,Access-Control-Request-Method,Access-Control-Request-Headers");
            resp.setHeader("Access-Control-Expose-Headers", "Location,Content-Disposition,Set-Cookie");
            resp.setHeader("Access-Control-Max-Age", "3600");
        }
    }

    /**
     * Đọc type từ query hoặc header (không phân biệt hoa thường).
     * Trả về INBOUND/OUTBOUND nếu hợp lệ, hoặc null nếu thiếu/không hợp lệ.
     */
    private static String readType(HttpServletRequest req) {
        String t = req.getParameter("type");
        if (t == null || t.isEmpty()) {
            t = req.getHeader("X-Doc-Type");
        }
        if (t == null) return null;
        t = t.trim().toUpperCase();
        if ("INBOUND".equals(t) || "OUTBOUND".equals(t)) return t;
        return null; // coi như không hợp lệ
    }

    // Chỉ preview vài trường quan trọng, cắt ngắn để không lộ dữ liệu nhạy cảm
    private String safePreviewJson(JsonNode root, int maxLen) {
        try {
            ObjectNode preview = om.createObjectNode();
            if (root.has("type")) preview.set("type", root.get("type"));
            if (root.has("number")) preview.put("number", safe(root.get("number").asText()));
            if (root.has("issued_at")) preview.put("issued_at", safe(root.get("issued_at").asText()));
            if (root.has("received_at")) preview.put("received_at", safe(root.get("received_at").asText()));
            if (root.has("recipient_unit")) preview.put("recipient_unit", safe(root.get("recipient_unit").asText()));
            if (root.has("sender_unit")) preview.put("sender_unit", safe(root.get("sender_unit").asText()));
            if (root.has("attachments")) preview.put("attachments_count", root.get("attachments").size());
            String s = preview.toString();
            return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
        } catch (Exception e) {
            return "{preview_error}";
        }
    }

    private static String safe(String s) {
        if (s == null) return "null";
        String t = s.trim();
        return (t.length() > 80) ? t.substring(0, 77) + "..." : t;
    }
}
