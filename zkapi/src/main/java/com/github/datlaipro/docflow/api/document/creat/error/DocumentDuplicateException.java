package com.github.datlaipro.docflow.api.document.creat.error;

import com.github.datlaipro.docflow.api.shared.error.AppException;
import com.github.datlaipro.docflow.api.shared.error.ErrorCode;

/** Trùng số hiệu (unique (doc_type, doc_number)) hoặc trùng logic nghiệp vụ. */
public class DocumentDuplicateException extends AppException {
    public DocumentDuplicateException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message); // dùng VALIDATION_ERROR cho trùng dữ liệu đầu vào
    }
}
