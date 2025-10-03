package com.github.datlaipro.docflow.api.document.creat.repo;


import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.*;
import java.util.List;
import java.util.Objects;

/**
 * DAO tạo văn bản Đi/Đến với JDBC (MySQL 5.7).
 * - Đặt @actor_id (session var) để trigger ghi history "CREATE".
 * - Insert vào documents và (tuỳ chọn) document_attachments trong cùng transaction.
 * - Trả về id của document vừa tạo.
 *
 * Yêu cầu schema:
 *   documents(id PK AI, doc_type, doc_number, title, content, status, originator_id,
 *             current_handler_id, issued_at, received_at, sender_unit, recipient_unit, created_at, updated_at)
 *   document_attachments(id PK AI, document_id, file_name, file_url, mime_type, size_bytes, uploaded_by, uploaded_at)
 */
public class DocumentDao {

    private final DataSource ds;

    public DocumentDao() {
        try {
            this.ds = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/docflow");
        } catch (NamingException e) {
            throw new RuntimeException("Cannot lookup DataSource jdbc/docflow", e);
        }
    }

    // --- API công khai -------------------------------------------------------

    /** Tạo văn bản Đến (INBOUND). receivedAt/senderUnit bắt buộc theo nghiệp vụ. */
    public long createInbound(
            String docNumber,
            String title,
            String content,
            long originatorId,
            Long currentHandlerId,         // có thể null
            Date receivedAt,               // ngày nhận
            String senderUnit,             // nơi gửi
            List<Attachment> attachments,  // có thể null/empty
            long actorId                   // người thực hiện thao tác (ghi vào history qua trigger)
    ) throws SQLException {
        Objects.requireNonNull(receivedAt, "receivedAt is required for INBOUND");
        Objects.requireNonNull(senderUnit, "senderUnit is required for INBOUND");
        return createInternal(
                DocType.INBOUND, docNumber, title, content,
                originatorId, currentHandlerId,
                /*issuedAt*/ null, /*receivedAt*/ receivedAt,
                /*senderUnit*/ senderUnit, /*recipientUnit*/ null,
                attachments, actorId
        );
    }

    /** Tạo văn bản Đi (OUTBOUND). issuedAt/recipientUnit bắt buộc theo nghiệp vụ. */
    public long createOutbound(
            String docNumber,
            String title,
            String content,
            long originatorId,
            Long currentHandlerId,         // có thể null
            Date issuedAt,                 // ngày ban hành
            String recipientUnit,          // nơi nhận
            List<Attachment> attachments,  // có thể null/empty
            long actorId                   // người thực hiện thao tác (ghi vào history qua trigger)
    ) throws SQLException {
        Objects.requireNonNull(issuedAt, "issuedAt is required for OUTBOUND");
        Objects.requireNonNull(recipientUnit, "recipientUnit is required for OUTBOUND");
        return createInternal(
                DocType.OUTBOUND, docNumber, title, content,
                originatorId, currentHandlerId,
                /*issuedAt*/ issuedAt, /*receivedAt*/ null,
                /*senderUnit*/ null, /*recipientUnit*/ recipientUnit,
                attachments, actorId
        );
    }

    // --- Triển khai chung ----------------------------------------------------

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
            long actorId
    ) throws SQLException {

        // (1) Kiểm tra tối thiểu
        requireNonBlank(docNumber, "docNumber");
        requireNonBlank(title, "title");
        requireNonBlank(content, "content");

        // (2) Giao dịch
        try (Connection c = ds.getConnection()) {
            boolean oldAuto = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                // 2.1 Set biến phiên @actor_id => trigger sẽ dùng để ghi document_history (action=CREATE)
                try (PreparedStatement ps = c.prepareStatement("SET @actor_id = ?")) {
                    ps.setLong(1, actorId);
                    ps.executeUpdate();
                }

                // 2.2 Insert vào documents
                long docId;
                final String sqlDoc =
                        "INSERT INTO documents (doc_type, doc_number, title, content, status, " +
                        "originator_id, current_handler_id, issued_at, received_at, sender_unit, recipient_unit) " +
                        "VALUES (?, ?, ?, ?, 'PENDING', ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = c.prepareStatement(sqlDoc, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, docType.name());         // enum('OUTBOUND','INBOUND')  :contentReference[oaicite:3]{index=3}
                    ps.setString(2, docNumber);              // varchar(100)               :contentReference[oaicite:4]{index=4}
                    ps.setString(3, title);                  // varchar(500)               :contentReference[oaicite:5]{index=5}
                    ps.setString(4, content);                // mediumtext                 :contentReference[oaicite:6]{index=6}
                    ps.setLong(5, originatorId);             // NOT NULL FK users          :contentReference[oaicite:7]{index=7}
                    if (currentHandlerId == null) {
                        ps.setNull(6, Types.BIGINT);
                    } else {
                        ps.setLong(6, currentHandlerId);     // NULLable FK users          :contentReference[oaicite:8]{index=8}
                    }
                    // ngày theo hướng Đi/Đến
                    if (issuedAt == null) ps.setNull(7, Types.DATE); else ps.setDate(7, issuedAt);       // issued_at   :contentReference[oaicite:9]{index=9}
                    if (receivedAt == null) ps.setNull(8, Types.DATE); else ps.setDate(8, receivedAt);   // received_at :contentReference[oaicite:10]{index=10}
                    // nơi gửi/nhận (nullable)
                    if (senderUnit == null) ps.setNull(9, Types.VARCHAR); else ps.setString(9, senderUnit);         // sender_unit    :contentReference[oaicite:11]{index=11}
                    if (recipientUnit == null) ps.setNull(10, Types.VARCHAR); else ps.setString(10, recipientUnit); // recipient_unit :contentReference[oaicite:12]{index=12}

                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) throw new SQLException("No generated key for documents");
                        docId = rs.getLong(1);
                    }
                }

                // 2.3 (Tuỳ chọn) Insert attachments
                if (attachments != null && !attachments.isEmpty()) {
                    final String sqlAtt =
                            "INSERT INTO document_attachments (document_id, file_name, file_url, mime_type, size_bytes, uploaded_by) " +
                            "VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = c.prepareStatement(sqlAtt)) {
                        for (Attachment a : attachments) {
                            ps.setLong(1, docId);                // FK -> documents.id       :contentReference[oaicite:13]{index=13}
                            ps.setString(2, a.fileName);         // varchar(255)             :contentReference[oaicite:14]{index=14}
                            ps.setString(3, a.fileUrl);          // varchar(1024)            :contentReference[oaicite:15]{index=15}
                            if (a.mimeType == null) ps.setNull(4, Types.VARCHAR); else ps.setString(4, a.mimeType); // nullable       :contentReference[oaicite:16]{index=16}
                            if (a.sizeBytes == null) ps.setNull(5, Types.BIGINT); else ps.setLong(5, a.sizeBytes);  // nullable       :contentReference[oaicite:17]{index=17}
                            ps.setLong(6, a.uploadedByUserId);   // NOT NULL FK users        :contentReference[oaicite:18]{index=18}
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }

                c.commit();
                c.setAutoCommit(oldAuto);
                return docId;

            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                // clear biến phiên để tránh “rò” sang request khác trong cùng pool connection
                try (PreparedStatement ps = c.prepareStatement("SET @actor_id = NULL")) {
                    ps.executeUpdate();
                } catch (SQLException ignore) {}
            }
        }
    }

    // --- Helper --------------------------------------------------------------

    private static void requireNonBlank(String s, String field) {
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    public enum DocType { OUTBOUND, INBOUND } // bám enum trong DB  :contentReference[oaicite:19]{index=19}

    /** DTO nhẹ cho file đính kèm khi tạo mới. */
    public static class Attachment {
        public final String fileName;         // bắt buộc
        public final String fileUrl;          // bắt buộc (đường dẫn/R2/S3…)
        public final String mimeType;         // tuỳ chọn
        public final Long sizeBytes;          // tuỳ chọn
        public final long uploadedByUserId;   // bắt buộc

        public Attachment(String fileName, String fileUrl, String mimeType, Long sizeBytes, long uploadedByUserId) {
            this.fileName = fileName;
            this.fileUrl = fileUrl;
            this.mimeType = mimeType;
            this.sizeBytes = sizeBytes;
            this.uploadedByUserId = uploadedByUserId;
        }
    }
}
