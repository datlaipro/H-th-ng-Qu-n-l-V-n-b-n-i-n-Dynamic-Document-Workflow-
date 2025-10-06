package com.github.datlaipro.docflow.api.document.creat.dto;

import java.util.List;

/** Request tạo văn bản Đến (INBOUND). */
public class DocumentReqIN {
    /** Số hiệu, ví dụ "0B-2025-01" */
    public String type; // luôn là "INBOUND"
    public String number;
    public String title;
    public String content;
    /** Ngày nhận: yyyy-MM-dd */
    public String receivedAt;
    /** Nơi gửi */
    public String senderUnit;
    public List<AttachmentReq> attachments;

    public static class AttachmentReq {
        public String fileName;
        public String fileUrl;
        public String mimeType;
        public Long sizeBytes;
    }
}
