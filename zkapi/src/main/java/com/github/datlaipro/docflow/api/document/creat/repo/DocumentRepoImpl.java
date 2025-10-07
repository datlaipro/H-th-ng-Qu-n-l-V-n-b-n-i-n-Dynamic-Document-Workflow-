package com.github.datlaipro.docflow.api.document.creat.repo;

import infra.Db;
import com.github.datlaipro.docflow.api.document.creat.repo.DocumentRepo.Attachment;

import java.sql.*;
import java.util.List;
import java.util.Objects;

public class DocumentRepoImpl implements DocumentRepo {

    enum DocType {
        OUTBOUND, INBOUND
    }

    @Override
    public long createInbound(String docNumber, String title, String content, long originatorId, Long currentHandlerId,
            Date receivedAt, String senderUnit,
            List<Attachment> attachments, long actorId) throws SQLException {
        Objects.requireNonNull(receivedAt, "receivedAt is required for INBOUND");
        Objects.requireNonNull(senderUnit, "senderUnit is required for INBOUND");
        return createInternal(
                DocType.INBOUND, docNumber, title, content,
                originatorId, currentHandlerId,
                /* issuedAt */ null, /* receivedAt */ receivedAt,
                /* senderUnit */ senderUnit, /* recipientUnit */ null,
                attachments, actorId);
    }

    @Override
    public long createOutbound(String docNumber, String title, String content, long originatorId, Long currentHandlerId,
            Date issuedAt, String recipientUnit,
            List<Attachment> attachments, long actorId) throws SQLException {
        Objects.requireNonNull(issuedAt, "issuedAt is required for OUTBOUND");
        Objects.requireNonNull(recipientUnit, "recipientUnit is required for OUTBOUND");
        return createInternal(
                DocType.OUTBOUND, docNumber, title, content,
                originatorId, currentHandlerId,
                /* issuedAt */ issuedAt, /* receivedAt */ null,
                /* senderUnit */ null, /* recipientUnit */ recipientUnit,
                attachments, actorId);
    }

    // ================== Core (dùng chung) ==================

    private long createInternal(
            DocType docType,
            String docNumber,
            String title,
            String content,
            long originatorId,
            Long currentHandlerId,
            Date issuedAt,
            Date receivedAt,
            String senderUnit,
            String recipientUnit,
            List<Attachment> attachments,
            long actorId) throws SQLException {

        requireNonBlank(docNumber, "docNumber");
        requireNonBlank(title, "title");
        requireNonBlank(content, "content");

        Connection c = null;
        boolean oldAuto = true;
        try {
            // Db.get() có thể throws Exception -> catch và bọc lại
            try {
                c = infra.Db.get();
            } catch (Exception e) {
                throw new SQLException("Failed to obtain DB connection", e);
            }

            oldAuto = c.getAutoCommit();
            c.setAutoCommit(false);

            // 1) Set biến phiên @actor_id
            try (PreparedStatement ps = c.prepareStatement("SET @actor_id = ?")) {
                ps.setLong(1, actorId);
                ps.executeUpdate();
            }

            // 2) Insert documents
            long docId;
            final String sqlDoc = "INSERT INTO documents (doc_type, doc_number, title, content, status, " +
                    "originator_id, current_handler_id, issued_at, received_at, sender_unit, recipient_unit) " +
                    "VALUES (?, ?, ?, ?, 'PENDING', ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = c.prepareStatement(sqlDoc, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, docType.name());
                ps.setString(2, docNumber);
                ps.setString(3, title);
                ps.setString(4, content);
                ps.setLong(5, originatorId);

                if (currentHandlerId == null)
                    ps.setNull(6, Types.BIGINT);
                else
                    ps.setLong(6, currentHandlerId);
                if (issuedAt == null)
                    ps.setNull(7, Types.DATE);
                else
                    ps.setDate(7, issuedAt);
                if (receivedAt == null)
                    ps.setNull(8, Types.DATE);
                else
                    ps.setDate(8, receivedAt);
                if (senderUnit == null)
                    ps.setNull(9, Types.VARCHAR);
                else
                    ps.setString(9, senderUnit);
                if (recipientUnit == null)
                    ps.setNull(10, Types.VARCHAR);
                else
                    ps.setString(10, recipientUnit);

                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next())
                        throw new SQLException("No generated key for documents");
                    docId = rs.getLong(1);
                }
            }

            // 3) Insert attachments (nếu có)
            if (attachments != null && !attachments.isEmpty()) {
                final String sqlAtt = "INSERT INTO document_attachments (document_id, file_name, file_url, mime_type, size_bytes, uploaded_by) "
                        +
                        "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = c.prepareStatement(sqlAtt)) {
                    for (Attachment a : attachments) {
                        ps.setLong(1, docId);
                        ps.setString(2, a.fileName);
                        ps.setString(3, a.fileUrl);
                        if (a.mimeType == null)
                            ps.setNull(4, Types.VARCHAR);
                        else
                            ps.setString(4, a.mimeType);
                        if (a.sizeBytes == null)
                            ps.setNull(5, Types.BIGINT);
                        else
                            ps.setLong(5, a.sizeBytes);
                        ps.setLong(6, a.uploadedByUserId);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            c.commit();
            return docId;

        } catch (SQLException ex) {
            if (c != null)
                try {
                    c.rollback();
                } catch (SQLException ignore) {
                }
            throw ex;
        } finally {
            // Clear biến phiên @actor_id
            if (c != null) {
                try (PreparedStatement ps = c.prepareStatement("SET @actor_id = NULL")) {
                    ps.executeUpdate();
                } catch (SQLException ignore) {
                }

                try {
                    c.setAutoCommit(oldAuto);
                } catch (SQLException ignore) {
                }
                try {
                    c.close();
                } catch (SQLException ignore) {
                }
            }
        }
    }

    private static void requireNonBlank(String s, String field) {
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
