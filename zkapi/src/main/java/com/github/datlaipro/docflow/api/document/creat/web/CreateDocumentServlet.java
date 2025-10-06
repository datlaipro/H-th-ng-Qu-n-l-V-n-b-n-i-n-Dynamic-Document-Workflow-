package com.github.datlaipro.docflow.api.document.creat.web;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.github.datlaipro.docflow.api.document.creat.AuthGuard;
import com.github.datlaipro.docflow.api.document.creat.dto.DocumentReqIN;
import com.github.datlaipro.docflow.api.document.creat.dto.DocumentReqOUT;
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
 * Hỗ trợ 2 payload:
 *  - INBOUND: DocumentReqIN
 *  - OUTBOUND: DocumentReqOUT
 *
 * Ưu tiên query/header "type" (INBOUND|OUTBOUND). Nếu không, autodetect theo trường trong JSON.
 */
@WebServlet(name = "CreateDocumentServlet", urlPatterns = {"/api/documents"})
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

        try (PrintWriter out = resp.getWriter()) {
            LOG.info(() -> String.format("[DOC][HTTP][REQ] %s %s CT=%s ORIGIN=%s", method, uri, ct, origin));

            // 👇 BIND request vào AuthGuard để service có request context
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
                LOG.fine(() -> "[DOC][HTTP][BODY] " + safePreviewJson(root, 400));

                // Xác định type
                String explicitType = readType(req);
                String resolvedType = explicitType != null ? explicitType : detectType(root);
                LOG.info(() -> String.format("[DOC][HTTP] explicitType=%s resolvedType=%s", explicitType, resolvedType));

                if (resolvedType == null) {
                    LOG.warning("[DOC][HTTP][ERR] Cannot detect type from body/headers");
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.write("{\"error\":\"Cannot detect document type. Provide 'type' (INBOUND|OUTBOUND) or include fields of the corresponding type.\"}");
                    return;
                }

                long id;
                switch (resolvedType) {
                    case "INBOUND": {
                        DocumentReqIN body = om.treeToValue(root, DocumentReqIN.class);
                        LOG.fine(() -> String.format("[DOC][INBOUND] number=%s receivedAt=%s senderUnit=%s attCount=%d",
                                safe(body.number), safe(body.receivedAt), safe(body.senderUnit),
                                body.attachments == null ? 0 : body.attachments.size()));
                        id = service.createInbound(body);
                        break;
                    }
                    case "OUTBOUND": {
                        DocumentReqOUT body = om.treeToValue(root, DocumentReqOUT.class);
                        LOG.fine(() -> String.format("[DOC][OUTBOUND] number=%s issuedAt=%s recipientUnit=%s attCount=%d",
                                safe(body.number), safe(body.issuedAt), safe(body.recipientUnit),
                                body.attachments == null ? 0 : body.attachments.size()));
                        id = service.createOutbound(body);
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
                LOG.info(() -> String.format("[DOC][HTTP][OK] id=%d type=%s in %d ms", id, resolvedType, dt));

                resp.setStatus(HttpServletResponse.SC_OK);
                out.write("{\"id\":" + id + ",\"message\":\"created\"}");
            } finally {
                // 👇 luôn clear để tránh leak ThreadLocal giữa các request
                AuthGuard.clear();
            }
        } catch (IllegalArgumentException ex) {
            LOG.log(Level.WARNING, "[DOC][HTTP][BAD_REQUEST] " + ex.getMessage(), ex);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"" + escape(ex.getMessage()) + "\"}");
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "[DOC][HTTP][EX] " + ex.getMessage(), ex);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"Internal error: " + escape(ex.getMessage()) + "\"}");
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

    /** Đọc type từ query hoặc header (không phân biệt hoa thường), trả về INBOUND/OUTBOUND hoặc null */
    private static String readType(HttpServletRequest req) {
        String t = req.getParameter("type");
        if (t == null || t.isEmpty()) {
            t = req.getHeader("X-Doc-Type");
        }
        if (t == null) return null;
        t = t.trim().toUpperCase();
        if ("INBOUND".equals(t) || "OUTBOUND".equals(t)) return t;
        return null;
    }

    /** Tự nhận dạng loại theo trường trong JSON */
    private static String detectType(JsonNode root) {
        boolean looksInbound  = root.hasNonNull("received_at")  || root.hasNonNull("sender_unit");
        boolean looksOutbound = root.hasNonNull("issued_at")    || root.hasNonNull("recipient_unit");

        if (looksInbound && !looksOutbound) return "INBOUND";
        if (looksOutbound && !looksInbound) return "OUTBOUND";
        // Nếu cả hai hoặc không cái nào rõ ràng → trả null, yêu cầu client chỉ rõ
        return null;
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
