package com.github.datlaipro.docflow.api.document.creat.repo;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

public interface DocumentRepo {

    long createInbound(
            String docNumber,
            String title,
            String content,
            long originatorId,
            Long currentHandlerId,   // nullable
            Date receivedAt,         // bắt buộc cho INBOUND
            String senderUnit,       // bắt buộc cho INBOUND
            List<Attachment> attachments, // nullable/empty
            long actorId             // người thực hiện thao tác (ghi history)
    ) throws SQLException;

    long createOutbound(
            String docNumber,
            String title,
            String content,
            long originatorId,
            Long currentHandlerId,   // nullable
            Date issuedAt,           // bắt buộc cho OUTBOUND
            String recipientUnit,    // bắt buộc cho OUTBOUND
            List<Attachment> attachments, // nullable/empty
            long actorId
    ) throws SQLException;

    /** DTO nhẹ cho file đính kèm khi tạo mới. */
    final class Attachment {
        public final String fileName;
        public final String fileUrl;
        public final String mimeType;   // nullable
        public final Long sizeBytes;    // nullable
        public final long uploadedByUserId;

        public Attachment(String fileName, String fileUrl, String mimeType, Long sizeBytes, long uploadedByUserId) {
            this.fileName = fileName;
            this.fileUrl = fileUrl;
            this.mimeType = mimeType;
            this.sizeBytes = sizeBytes;
            this.uploadedByUserId = uploadedByUserId;
        }
    }
}
