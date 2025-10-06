package com.github.datlaipro.docflow.api.document.creat.error;


import com.github.datlaipro.docflow.api.shared.error.AppException;
import com.github.datlaipro.docflow.api.shared.error.ErrorCode;

/** Lỗi dữ liệu đầu vào không hợp lệ (thiếu field, sai định dạng...). */
public class DocumentValidationException extends AppException {
    public DocumentValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
    public DocumentValidationException(String message, Throwable cause) {
        super(ErrorCode.VALIDATION_ERROR, message, cause);
    }
}

