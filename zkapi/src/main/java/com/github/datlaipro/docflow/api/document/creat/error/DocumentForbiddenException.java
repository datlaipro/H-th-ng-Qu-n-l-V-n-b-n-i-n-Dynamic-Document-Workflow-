package com.github.datlaipro.docflow.api.document.creat.error;

import com.github.datlaipro.docflow.api.shared.error.AppException;
import com.github.datlaipro.docflow.api.shared.error.ErrorCode;

/** Không có quyền hoặc chưa đăng nhập. */
public class DocumentForbiddenException extends AppException {
    public DocumentForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }
}
