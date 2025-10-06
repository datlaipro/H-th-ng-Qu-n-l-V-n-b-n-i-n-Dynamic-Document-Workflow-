package com.github.datlaipro.docflow.api.document.creat.error;

import com.github.datlaipro.docflow.api.shared.error.AppException;
import com.github.datlaipro.docflow.api.shared.error.ErrorCode;

/** Không tìm thấy tài nguyên (khi tra cứu doc vừa tạo hoặc tham chiếu liên quan). */
public class DocumentNotFoundException extends AppException {
    public DocumentNotFoundException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}
