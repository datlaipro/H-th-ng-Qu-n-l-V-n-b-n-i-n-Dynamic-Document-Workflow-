package com.github.datlaipro.docflow.api.document.creat.service;

import com.github.datlaipro.docflow.api.document.creat.AuthGuard.AuthGuard;
import com.github.datlaipro.docflow.api.document.creat.dto.DocumentReqIN;
import com.github.datlaipro.docflow.api.document.creat.dto.DocumentReqOUT;
import com.github.datlaipro.docflow.api.document.creat.error.DocumentErrorMapper;
import com.github.datlaipro.docflow.api.document.creat.error.DocumentValidationException;

import com.github.datlaipro.docflow.api.document.creat.repo.DocumentRepo;
import com.github.datlaipro.docflow.api.document.creat.repo.DocumentRepoImpl;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class DocumentCreateServiceImpl implements DocumentCreateService {
    private static final Logger LOG = Logger.getLogger(DocumentCreateServiceImpl.class.getName());

    private final DocumentRepo documentRepo = new DocumentRepoImpl();
    private static final Pattern DOC_NO = Pattern.compile("^[A-Z0-9]{2}-\\d{4}-\\d{2}$");

    @Override
    public long createOutbound(DocumentReqOUT req) throws Exception {
        final long actorId = AuthGuard.requireUserId(); // buộc đăng nhập

        // ===== LOG: bắt đầu =====
        LOG.info(() -> String.format("[DOC][OUTBOUND][START] actorId=%d, rawNumber=%s, issuedAt=%s, recipientUnit=%s, attCount=%d",
                actorId,
                safePreview(req != null ? req.number : null),
                safePreview(req != null ? req.issuedAt : null),
                safePreview(req != null ? req.recipientUnit : null),
                countAtt(req)));

        require(req, "Body");
        require(req.number, "number");
        require(req.title, "title");
        require(req.content, "content");
        require(req.issuedAt, "issuedAt");
        require(req.recipientUnit, "recipientUnit");

        final String number = req.number.trim().toUpperCase();
        if (!DOC_NO.matcher(number).matches()) {
            LOG.warning(() -> String.format("[DOC][OUTBOUND][VALIDATE] invalid number format: %s", number));
            throw new DocumentValidationException("Số hiệu không đúng dạng (VD: 0B-2025-01)");
        }

        final Date issuedAt = safeDate(req.issuedAt, "issuedAt");
        final String title = req.title.trim();
        final String content = req.content.trim();
        final String recipientUnit = req.recipientUnit.trim();

        final List<DocumentRepo.Attachment> atts = mapAttachments(req, actorId);

        try {
            LOG.info(() -> String.format("[DOC][OUTBOUND][REPO] inserting number=%s, issuedAt=%s, attCount=%d",
                    number, issuedAt, atts != null ? atts.size() : 0));

            long id = documentRepo.createOutbound(
                    number,
                    title,
                    content,
                    /* originatorId */ actorId,
                    /* currentHandlerId */ null,
                    issuedAt,
                    recipientUnit,
                    atts,
                    /* actorId */ actorId);

            LOG.info(() -> String.format("[DOC][OUTBOUND][OK] id=%d, number=%s, actorId=%d", id, number, actorId));
            return id;
        } catch (Throwable t) {
            LOG.log(Level.WARNING,
                    String.format("[DOC][OUTBOUND][ERR] number=%s, actorId=%d, msg=%s", number, actorId, t.getMessage()),
                    t);
            throw DocumentErrorMapper.map(t, "OUTBOUND", number);
        }
    }

    @Override
    public long createInbound(DocumentReqIN req) throws Exception {
        final long actorId = AuthGuard.requireUserId(); // buộc đăng nhập

        // ===== LOG: bắt đầu =====
        LOG.info(() -> String.format("[DOC][INBOUND][START] actorId=%d, rawNumber=%s, receivedAt=%s, senderUnit=%s, attCount=%d",
                actorId,
                safePreview(req != null ? req.number : null),
                safePreview(req != null ? req.receivedAt : null),
                safePreview(req != null ? req.senderUnit : null),
                countAtt(req)));

        require(req, "Body");
        require(req.number, "number");
        require(req.title, "title");
        require(req.content, "content");
        require(req.receivedAt, "receivedAt");
        require(req.senderUnit, "senderUnit");

        final String number = req.number.trim().toUpperCase();
        if (!DOC_NO.matcher(number).matches()) {
            LOG.warning(() -> String.format("[DOC][INBOUND][VALIDATE] invalid number format: %s", number));
            throw new DocumentValidationException("Số hiệu không đúng dạng (VD: 0B-2025-01)");
        }

        final Date receivedAt = safeDate(req.receivedAt, "receivedAt");
        final String title = req.title.trim();
        final String content = req.content.trim();
        final String senderUnit = req.senderUnit.trim();

        final List<DocumentRepo.Attachment> atts = mapAttachments(req, actorId);

        try {
            LOG.info(() -> String.format("[DOC][INBOUND][REPO] inserting number=%s, receivedAt=%s, attCount=%d",
                    number, receivedAt, atts != null ? atts.size() : 0));

            long id = documentRepo.createInbound(
                    number,
                    title,
                    content,
                    /* originatorId */ actorId,
                    /* currentHandlerId */ null,
                    receivedAt,
                    senderUnit,
                    atts,
                    /* actorId */ actorId);

            LOG.info(() -> String.format("[DOC][INBOUND][OK] id=%d, number=%s, actorId=%d", id, number, actorId));
            return id;
        } catch (Throwable t) {
            LOG.log(Level.WARNING,
                    String.format("[DOC][INBOUND][ERR] number=%s, actorId=%d, msg=%s", number, actorId, t.getMessage()),
                    t);
            throw DocumentErrorMapper.map(t, "INBOUND", number);
        }
    }

    // ---------- helpers ----------
    private static java.sql.Date safeDate(String yyyyMMdd, String field) {
        try { return java.sql.Date.valueOf(yyyyMMdd.trim()); }
        catch (Exception e) { throw new DocumentValidationException(field + " phải có dạng yyyy-MM-dd", e); }
    }

    // OVERLOAD cho OUTBOUND
    private static List<DocumentRepo.Attachment> mapAttachments(
            com.github.datlaipro.docflow.api.document.creat.dto.DocumentReqOUT req,
            long actorId) {

        List<DocumentRepo.Attachment> atts = new ArrayList<>();
        if (req.attachments != null) {
            for (com.github.datlaipro.docflow.api.document.creat.dto.DocumentReqOUT.AttachmentReq a : req.attachments) {
                if (a == null || isBlank(a.fileName) || isBlank(a.fileUrl)) continue;
                atts.add(new DocumentRepo.Attachment(
                        a.fileName.trim(),
                        a.fileUrl.trim(),
                        isBlank(a.mimeType) ? null : a.mimeType.trim(),
                        a.sizeBytes,
                        actorId));
            }
        }
        return atts;
    }

    // OVERLOAD cho INBOUND
    private static List<DocumentRepo.Attachment> mapAttachments(
            com.github.datlaipro.docflow.api.document.creat.dto.DocumentReqIN req,
            long actorId) {

        List<DocumentRepo.Attachment> atts = new ArrayList<>();
        if (req.attachments != null) {
            for (com.github.datlaipro.docflow.api.document.creat.dto.DocumentReqIN.AttachmentReq a : req.attachments) {
                if (a == null || isBlank(a.fileName) || isBlank(a.fileUrl)) continue;
                atts.add(new DocumentRepo.Attachment(
                        a.fileName.trim(),
                        a.fileUrl.trim(),
                        isBlank(a.mimeType) ? null : a.mimeType.trim(),
                        a.sizeBytes,
                        actorId));
            }
        }
        return atts;
    }

    private static void require(Object v, String f) {
        if (v == null || (v instanceof String && ((String) v).trim().isEmpty()))
            throw new DocumentValidationException(f + " là bắt buộc");
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private static int countAtt(DocumentReqOUT req) {
        return (req != null && req.attachments != null) ? req.attachments.size() : 0;
    }

    private static int countAtt(DocumentReqIN req) {
        return (req != null && req.attachments != null) ? req.attachments.size() : 0;
    }

    private static String safePreview(String s) {
        if (s == null) return "null";
        String t = s.trim();
        if (t.length() > 40) t = t.substring(0, 37) + "...";
        return t;
    }
}
