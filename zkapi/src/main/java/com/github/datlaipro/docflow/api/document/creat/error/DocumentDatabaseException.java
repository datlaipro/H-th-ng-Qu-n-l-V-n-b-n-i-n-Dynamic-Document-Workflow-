package com.github.datlaipro.docflow.api.document.creat.error;

import com.github.datlaipro.docflow.api.shared.error.AppException;
import com.github.datlaipro.docflow.api.shared.error.ErrorCode;

/** Lỗi DB chung (driver/connection/timeout...) */
public class DocumentDatabaseException extends AppException {
    public DocumentDatabaseException(String message, Throwable cause) {
        super(ErrorCode.DATABASE_ERROR, message, cause);
    }
}
