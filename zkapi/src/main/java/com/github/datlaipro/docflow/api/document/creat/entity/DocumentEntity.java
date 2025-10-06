package com.github.datlaipro.docflow.api.document.creat.entity;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;

/** Entity cho bảng documents (JDBC, không JPA). */
public class DocumentEntity {

    // ===== Enums bám schema =====
    public enum DocType {
        OUTBOUND, INBOUND
    } // Văn bản Đi | Đến

    public enum Status {
        PENDING, IN_PROGRESS, APPROVED, REJECTED, COMPLETED
    }

    // ===== Cột trong bảng documents =====
    private long id;                 // PK
    private DocType docType;         // enum('OUTBOUND','INBOUND')
    private String docNumber;        // số hiệu
    private String title;            // tiêu đề
    private String content;          // nội dung
    private Status status;           // trạng thái
    private long originatorId;       // người khởi tạo
    private Long currentHandlerId;   // người đang xử lý (nullable)
    private Date issuedAt;           // ngày ban hành (OUTBOUND) - nullable
    private Date receivedAt;         // ngày nhận (INBOUND) - nullable
    private String senderUnit;       // nơi gửi (INBOUND) - nullable
    private String recipientUnit;    // nơi nhận (OUTBOUND) - nullable
    private Timestamp createdAt;     // mặc định CURRENT_TIMESTAMP
    private Timestamp updatedAt;     // on update CURRENT_TIMESTAMP

    // ===== Constructors =====
    public DocumentEntity() {}

    public DocumentEntity(long id, DocType docType, String docNumber, String title, String content, Status status,
                          long originatorId, Long currentHandlerId, Date issuedAt, Date receivedAt,
                          String senderUnit, String recipientUnit,
                          Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.docType = docType;
        this.docNumber = docNumber;
        this.title = title;
        this.content = content;
        this.status = status;
        this.originatorId = originatorId;
        this.currentHandlerId = currentHandlerId;
        this.issuedAt = issuedAt;
        this.receivedAt = receivedAt;
        this.senderUnit = senderUnit;
        this.recipientUnit = recipientUnit;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ===== Static mapper từ JDBC =====
    public static DocumentEntity from(ResultSet rs) throws SQLException {
        DocumentEntity d = new DocumentEntity();
        String type = rs.getString("doc_type");
        d.docType = type != null ? DocType.valueOf(type) : null;

        d.id = rs.getLong("id");
        d.docNumber = rs.getString("doc_number");
        d.title = rs.getString("title");
        d.content = rs.getString("content");

        String st = rs.getString("status");
        d.status = st != null ? Status.valueOf(st) : null;

        d.originatorId = rs.getLong("originator_id");
        long ch = rs.getLong("current_handler_id");
        d.currentHandlerId = rs.wasNull() ? null : ch;

        d.issuedAt = rs.getDate("issued_at");
        d.receivedAt = rs.getDate("received_at");
        d.senderUnit = rs.getString("sender_unit");
        d.recipientUnit = rs.getString("recipient_unit");

        d.createdAt = rs.getTimestamp("created_at");
        d.updatedAt = rs.getTimestamp("updated_at");
        return d;
    }

    // ===== Getters/Setters =====
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public DocType getDocType() { return docType; }
    public void setDocType(DocType docType) { this.docType = docType; }

    public String getDocNumber() { return docNumber; }
    public void setDocNumber(String docNumber) { this.docNumber = docNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public long getOriginatorId() { return originatorId; }
    public void setOriginatorId(long originatorId) { this.originatorId = originatorId; }

    public Long getCurrentHandlerId() { return currentHandlerId; }
    public void setCurrentHandlerId(Long currentHandlerId) { this.currentHandlerId = currentHandlerId; }

    public Date getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Date issuedAt) { this.issuedAt = issuedAt; }

    public Date getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Date receivedAt) { this.receivedAt = receivedAt; }

    public String getSenderUnit() { return senderUnit; }
    public void setSenderUnit(String senderUnit) { this.senderUnit = senderUnit; }

    public String getRecipientUnit() { return recipientUnit; }
    public void setRecipientUnit(String recipientUnit) { this.recipientUnit = recipientUnit; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    // ===== equals/hashCode theo id =====
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentEntity)) return false;
        DocumentEntity that = (DocumentEntity) o;
        return id == that.id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "DocumentEntity{id=" + id + ", type=" + docType + ", number='" + docNumber + "', title='" + title + "'}";
    }
}
