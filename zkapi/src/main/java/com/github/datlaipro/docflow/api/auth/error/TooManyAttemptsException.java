package com.github.datlaipro.docflow.api.auth.error;

import com.github.datlaipro.docflow.api.shared.error.*;

public class TooManyAttemptsException extends AppException {
    public TooManyAttemptsException(int retryAfterSec) {
        super(ErrorCode.TOO_MANY_ATTEMPTS, "Too many failed attempts. Try again later.", retryAfterSec);
    }
}
