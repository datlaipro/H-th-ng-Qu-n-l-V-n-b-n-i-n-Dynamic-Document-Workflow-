// DocumentReqIN.java
package com.github.datlaipro.docflow.api.document.creat.dto;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;

/** Request tạo văn bản Đến (INBOUND). */
public class DocumentReqIN {
    /** luôn là "INBOUND" */
    @NotBlank(message = "type là bắt buộc")
    @Pattern(regexp = "^INBOUND$", message = "type phải là INBOUND")
    public String type;

    /** Số hiệu, ví dụ "0B-2025-01" */
    @NotBlank(message = "number là bắt buộc")
    @Pattern(regexp = "^[A-Z0-9]{2}-\\d{4}-\\d{2}$", message = "number phải có dạng AB-2025-01")
    public String number;

    @NotBlank(message = "title là bắt buộc")
    @Size(max = 500, message = "title quá dài (<=500)")
    public String title;

    @NotBlank(message = "content là bắt buộc")
    public String content;

    /** yyyy-MM-dd */
    @NotBlank(message = "receivedAt là bắt buộc")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "receivedAt phải yyyy-MM-dd")
    public String receivedAt;

    @NotBlank(message = "senderUnit là bắt buộc")
    public String senderUnit;

    @Valid
    public List<AttachmentReq> attachments;

    public static class AttachmentReq {
        @NotBlank(message = "attachment.fileName là bắt buộc")
        public String fileName;

        @NotBlank(message = "attachment.fileUrl là bắt buộc")
        public String fileUrl;

        public String mimeType;

        @Min(value = 0, message = "attachment.sizeBytes phải >= 0")
        public Long sizeBytes;
    }
}
