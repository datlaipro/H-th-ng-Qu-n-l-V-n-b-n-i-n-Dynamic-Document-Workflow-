package com.github.datlaipro.docflow.api.document.creat.dto;


import java.util.List;

/** Request tạo văn bản Đến (INBOUND). */
public class DocumentReqOUT {
      /** Số hiệu, ví dụ "0B-2025-01" */
      public String type;  // luôn là "OUTBOUND"
    public String number;

    /** Tiêu đề */
    public String title;
    /** Nội dung */
    public String content;
    /** Ngày ban hành: yyyy-MM-dd */
    public String issuedAt;
    /** Nơi nhận */
    public String recipientUnit;
    /** File đính kèm (tuỳ chọn) */
    public List<AttachmentReq> attachments;

    public static class AttachmentReq {
        public String fileName;     // bắt buộc nếu có item
        public String fileUrl;      // URL public R2
        public String mimeType;     // ví dụ application/pdf
        public Long sizeBytes;      // tuỳ chọn
    }
}
